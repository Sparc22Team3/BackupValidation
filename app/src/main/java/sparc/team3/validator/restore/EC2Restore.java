package sparc.team3.validator.restore;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.DescribeRestoreJobRequest;
import software.amazon.awssdk.services.backup.model.DescribeRestoreJobResponse;
import software.amazon.awssdk.services.backup.model.GetRecoveryPointRestoreMetadataRequest;
import software.amazon.awssdk.services.backup.model.GetRecoveryPointRestoreMetadataResponse;
import software.amazon.awssdk.services.backup.model.ListRecoveryPointsByBackupVaultRequest;
import software.amazon.awssdk.services.backup.model.ListRecoveryPointsByBackupVaultResponse;
import software.amazon.awssdk.services.backup.model.RecoveryPointByBackupVault;
import software.amazon.awssdk.services.backup.model.StartRestoreJobRequest;
import software.amazon.awssdk.services.backup.model.StartRestoreJobResponse;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import sparc.team3.validator.util.InstanceSettings;

/**
 * sparc.team3.validator.restore.EC2Restore selects a recovery point to back up, restores the recovery point, and waits for the
 * recovered resource to pass initialization checks. 
 */
public class EC2Restore {

    private final BackupClient backupClient;
    private final Ec2Client ec2Client;
    private final InstanceSettings instanceSettings;
    private int recoveryNumber; 
    private final TreeMap<Instant, RecoveryPointByBackupVault> recoveryPoints;

    private RecoveryPointByBackupVault currentRecoveryPoint;
    private Map<String, String> metadata; 
    private Arn resourceArn;
    private final Logger logger;


    public EC2Restore(BackupClient backupClient, Ec2Client ec2Client, InstanceSettings instanceSettings, int recoveryNumber) throws Exception{
        logger = LoggerFactory.getLogger(EC2Restore.class);

        this.backupClient = backupClient;
        this.ec2Client = ec2Client;
        this.instanceSettings = instanceSettings;
        this.recoveryNumber = recoveryNumber;
        this.recoveryPoints = getRecoveryPoints(instanceSettings.getBackupVault());

        this.currentRecoveryPoint = getRecentRecoveryPoint(recoveryNumber);
        this.metadata = editRecoveryMeta(getRecoveryMetaData(currentRecoveryPoint));

    }
    
    /**
     * Method gets a list of backup restore points from backup vault and populates a sorted data structure. 
     * @param backupVaultName the string name of the backup vault to retrieve recovery points from
     * @return a TreeMap of the recovery points in the backup vault
     * 
     */
    private TreeMap<Instant, RecoveryPointByBackupVault> getRecoveryPoints(String backupVaultName){

        TreeMap<Instant, RecoveryPointByBackupVault> output =
                new TreeMap<>(Collections.reverseOrder());
        
        //call and response with amazon to get list of vault backups
        ListRecoveryPointsByBackupVaultRequest  request = 
        ListRecoveryPointsByBackupVaultRequest.builder().
        backupVaultName(backupVaultName).build();
        
        ListRecoveryPointsByBackupVaultResponse response = 
        backupClient.listRecoveryPointsByBackupVault(request);

        for(software.amazon.awssdk.services.backup.model.RecoveryPointByBackupVault r: 
        response.recoveryPoints()){

            output.put(r.creationDate(), r); 
        }

        return output; 
    }

    /**
     * Return most recent recovery point from vault.
     * @param recoveryNumber the number of the recovery point to get
     * @return a RecoveryPointByBackupVault
     * @throws Exception when recovery points have been exhausted
     */

    private RecoveryPointByBackupVault getRecentRecoveryPoint(int recoveryNumber) throws Exception{

        if (recoveryNumber > recoveryPoints.size()){

            throw new Exception("Recovery Points Exhausted"); 

        }
        
        return recoveryPoints.get(recoveryPoints.keySet().toArray()[recoveryNumber]); 
    }

    /**
     * Start the restore job given a recovery point. 
     * @param recoveryNumber the int of the recovery point to restore
     * @return a string of the response of the restore job
     */
    private String startRestore(int recoveryNumber){
        
        StartRestoreJobRequest request = StartRestoreJobRequest.builder().
        recoveryPointArn(currentRecoveryPoint.recoveryPointArn()).iamRoleArn(currentRecoveryPoint.iamRoleArn())
        .metadata(metadata).build();
        
        StartRestoreJobResponse response = backupClient.startRestoreJob(request);

        return response.restoreJobId();

    }

    /**
     * Gets metadata of recovery point for restore job request.
     * @param recoveryPoint the RecoveryPointByBackupVault to get metadata from
     * @return a Map of the string metadata
     */
    private  Map<String, String> getRecoveryMetaData(RecoveryPointByBackupVault recoveryPoint){

        Map<String, String> output;

        GetRecoveryPointRestoreMetadataRequest request = 
        GetRecoveryPointRestoreMetadataRequest.builder().
        recoveryPointArn(recoveryPoint.recoveryPointArn()).backupVaultName(instanceSettings.getBackupVault()).build();
        
        GetRecoveryPointRestoreMetadataResponse response = backupClient
        .getRecoveryPointRestoreMetadata(request);

        //Modify network settings
        output = response.restoreMetadata(); //this becomes immutable? Remove gets angry.

        return output; 
        
    }

    /**
     * Solves issue with metadata where restore job request does not work with
     * NetworkInterfaces if security group and subnet are specified. 
     * 
     * Also, CpuOptions causes restore job request to fail.
     * 
     * @param metaData a Map of string metadata
     * @return the edited meta data Map
     */

    private Map<String, String> editRecoveryMeta(Map<String, String> metaData){

        Map<String, String> output = new HashMap<>();

        for(Entry<String, String> entry: metaData.entrySet()){

            if(entry.getKey() != "NetworkInterfaces"){

                if(entry.getKey() == "CpuOptions"){continue;}

                output.put(entry.getKey(), entry.getValue()); 
            }

        }

        return output; 
    }

    /**
     * Polls AWS Backup to check when restore job is complete. Returns error if restore job took
     * longer than 10 minutes.
     * 
     * Throws error if job isn't completed within allotted time.
     * @return a string of the instance id
     * @throws Exception when the backup restore times out
     */
    public Instance restoreEC2FromBackup() throws Exception{

        String restoreJobId = startRestore(recoveryNumber); 

        int attempts = 0; 
        while(attempts < 11){
          
          try{
  
            //get restore job information and wait until status of restore job is "completed"
            DescribeRestoreJobRequest newRequest = DescribeRestoreJobRequest
            .builder().restoreJobId(restoreJobId).build(); 
            DescribeRestoreJobResponse restoreResult = backupClient.describeRestoreJob(newRequest);
  
            System.out.println("Restore Status:" + restoreResult.status().toString()); 
            
            if(restoreResult.status().toString() == "COMPLETED"){
              resourceArn = Arn.fromString(restoreResult.createdResourceArn());
              break; 
            }
  
            
          } catch(Exception e){
  
            System.err.println(e);
  
            System.exit(1); 
  
          }
          Thread.sleep(60000);
          attempts++; 
        }

        if (attempts >= 11){

            throw new Exception("Backup Restore Timeout");

        }

        return getInstance();

    }

    /**
     * Get the Instance information about the EC2 instance that was restored
     * @return an Instance describing the instance that was restored
     */
    private Instance getInstance(){
        String id = resourceArn.resource().resource();

        DescribeInstancesRequest instanceReq = DescribeInstancesRequest.builder().instanceIds(id).build();

        DescribeInstancesResponse descriptionRes = ec2Client.describeInstances(instanceReq);

        // Get first reservation in a list of reservations that should only have 1
        Reservation reservation = descriptionRes.reservations().get(0);
        if(descriptionRes.reservations().size() > 1)
            logger.warn("More than 1 reservation was returned");
        // Get first instance in list of instances that should only have 1
        Instance instance = reservation.instances().get(0);
        if(reservation.instances().size() > 1)
            logger.warn("More than 1 instance was returned");

        return instance;
    }
    
    /**
     * Parse resource ARN to obtain EC2 instanceId
     * @param resourceARN a resourceARN string to parse
     * @return the string of the instance id
     */
    private String parseInstanceId(String resourceARN){

        Pattern pattern = Pattern.compile("i-\\w+");
        Matcher matcher = pattern.matcher(resourceARN);
        String instanceId = ""; 
        
        if(matcher.find()){instanceId = matcher.group();}

        return instanceId; 
    }
    
    /**
     * Returns recovery points in sorted order. 
     * @return a TreeMap of recoveryPointsByBackupVault
     */
    public TreeMap<Instant, RecoveryPointByBackupVault> getAvailableRecoveryPoints (){

        return recoveryPoints;
    }

    /**
     * Returns current recovery number of object. 
     * @return the int of the current recovery number
     */
    public int getRecoveryPointNumber(){
        return recoveryNumber;
    }

    /**
     * Set current recovery number of object. 
     * @param recoveryNumber the int to set the number of the recovery point
     * @throws Exception when there is an issue getting the recent recovery point
     */
    public void setRecoveryPointNumber(int recoveryNumber) throws Exception{
        this.recoveryNumber=recoveryNumber;

        this.currentRecoveryPoint = getRecentRecoveryPoint(recoveryNumber);
        this.metadata = editRecoveryMeta(getRecoveryMetaData(currentRecoveryPoint));
    }

    public Arn getResourceArn(){
        return resourceArn;
    }
}