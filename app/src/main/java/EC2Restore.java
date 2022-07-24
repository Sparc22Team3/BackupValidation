import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/**
 * EC2Restore selects a recovery point to backup, restores the recovery point, and waits for the 
 * recovered resource to pass initialization checks. 
 */
public class EC2Restore {

    private BackupClient client; 
    private String backupVaultName;
    private int recoveryNumber; 
    private TreeMap<Instant, RecoveryPointByBackupVault> recoveryPoints; 

    private RecoveryPointByBackupVault currentRecoveryPoint;
    private Map<String, String> metadata; 
    private String resourceARN; 


    public EC2Restore(BackupClient client, String backupVaultName, int recoveryNumber) throws Exception{

        this.client = client; 
        this.backupVaultName = backupVaultName;
        this.recoveryNumber = recoveryNumber;
        this.recoveryPoints = getRecoveryPoints(backupVaultName);

        this.currentRecoveryPoint = getRecentRecoveryPoint(recoveryNumber);
        this.metadata = editRecoveryMeta(getRecoveryMetaData(currentRecoveryPoint));

    }
    
    /**
     * Method gets a list of backup restore points from backup vault and populates a sorted data structure. 
     * @param backupVaultName
     * @return
     * 
     */
    private TreeMap<Instant, RecoveryPointByBackupVault> getRecoveryPoints(String backupVaultName){

        TreeMap<Instant, RecoveryPointByBackupVault> output = 
        new TreeMap<Instant, RecoveryPointByBackupVault>(Collections.reverseOrder());
        
        //call and response with amazon to get list of vault backups
        ListRecoveryPointsByBackupVaultRequest  request = 
        ListRecoveryPointsByBackupVaultRequest.builder().
        backupVaultName(backupVaultName).build();
        
        ListRecoveryPointsByBackupVaultResponse response = 
        client.listRecoveryPointsByBackupVault(request); 

        for(software.amazon.awssdk.services.backup.model.RecoveryPointByBackupVault r: 
        response.recoveryPoints()){

            output.put(r.creationDate(), r); 
        }

        return output; 
    }

    /**
     * Return most recent recovery point from vault. 
     * @return
     * @throws Exception
     */

    private RecoveryPointByBackupVault getRecentRecoveryPoint(int recoveryNumber) throws Exception{

        if (recoveryNumber > recoveryPoints.size()){

            throw new Exception("Recovery Points Exhausted"); 

        }
        
        return recoveryPoints.get(recoveryPoints.keySet().toArray()[recoveryNumber]); 
    }

    /**
     * Start the restore job given a recovery point. 
     * @param recoveryPoint
     * @return
     * @throws Exception
     */
    private String startRestore(int recoveryNumber) throws Exception{
        
        StartRestoreJobRequest request = StartRestoreJobRequest.builder().
        recoveryPointArn(currentRecoveryPoint.recoveryPointArn()).iamRoleArn(currentRecoveryPoint.iamRoleArn())
        .metadata(metadata).build();
        
        StartRestoreJobResponse response = client.startRestoreJob(request); 

        return response.restoreJobId();

    }

    /**
     * Gets meta data of recovery point for restore job request. 
     * @param recoveryPoint
     * @return
     */
    private  Map<String, String> getRecoveryMetaData(RecoveryPointByBackupVault recoveryPoint){

        Map<String, String> output;

        GetRecoveryPointRestoreMetadataRequest request = 
        GetRecoveryPointRestoreMetadataRequest.builder().
        recoveryPointArn(recoveryPoint.recoveryPointArn()).backupVaultName(backupVaultName).build();
        
        GetRecoveryPointRestoreMetadataResponse response = client
        .getRecoveryPointRestoreMetadata(request);

        //Modify network settings
        output = response.restoreMetadata(); //this becomes immutable? Remove gets angry.

        return output; 
        
    }

    /**
     * Solves issue with meta data where restore job request does not work with 
     * NetworkInterfaces if security group and subnet are specified. 
     * 
     * Also CpuOptions causes restore job request to fail. 
     * 
     * @param metaData
     * @return
     */

    private Map<String, String> editRecoveryMeta(Map<String, String> metaData){

        Map<String, String> output = new HashMap<String, String>(); 

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
     * Throws error if job isn't completed within alotted time. 
     * @return
     * @throws Exception
     */
    public String restoreEC2FromBackup() throws Exception{

        String restoreJobId = startRestore(recoveryNumber); 

        int attempts = 0; 
        while(attempts < 11){
          
          try{
  
            //get restore job information and wait until status of restore job is "completed"
            DescribeRestoreJobRequest newRequest = DescribeRestoreJobRequest
            .builder().restoreJobId(restoreJobId).build(); 
            DescribeRestoreJobResponse restoreResult = client.describeRestoreJob(newRequest);
  
            System.out.println("Restore Status:" + restoreResult.status().toString()); 
            
            if(restoreResult.status().toString() == "COMPLETED"){
              resourceARN = restoreResult.createdResourceArn();
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

        return parseInstanceId(resourceARN); 

    }
    
    /**
     * Parse resource ARN to obtain EC2 instanceId
     * @param resourceARN
     * @return
     */
    private String parseInstanceId(String resourceARN){

        Pattern pattern = Pattern.compile("i-\\w+");
        Matcher matcher = pattern.matcher(resourceARN.toString()); 
        String instanceId = ""; 
        
        if(matcher.find()){instanceId = matcher.group();}

        return instanceId; 
    }
    
    /**
     * Returns recovery points in sorted order. 
     * @return
     */
    public TreeMap<Instant, RecoveryPointByBackupVault> getAvailableRecoveryPoints (){

        return recoveryPoints;
    }

    /**
     * Returns current recovery number of object. 
     * @return
     */
    public int getRecoveryPointNumber(){
        return recoveryNumber;
    }

    /**
     * Set current recovery number of object. 
     * @param number
     * @throws Exception
     */
    public void setRecoveryPointNumber(int recoveryNumber) throws Exception{
        this.recoveryNumber=recoveryNumber;

        this.currentRecoveryPoint = getRecentRecoveryPoint(recoveryNumber);
        this.metadata = editRecoveryMeta(getRecoveryMetaData(currentRecoveryPoint));
    }

    public String getResourceARN(){
        return resourceARN;
    }
}