import java.time.Instant;
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

    BackupClient client; 
    String backupVaultName;
    int recoveryNumber; 
    TreeMap<Instant, RecoveryPointByBackupVault> recoveryPoints; 


    public EC2Restore(BackupClient client, String backupVaultName, int recoveryAttempt){

        this.client = client; 
        this.backupVaultName = backupVaultName;
        recoveryNumber = recoveryAttempt;
        this.recoveryPoints = getRecoveryPoints(backupVaultName);

    }
    
    /**
     * Method gets a list of backup restore points from backup vault and populates a sorted data structure. 
     * @param backupVaultName
     * @return
     * 
     */
    public TreeMap<Instant, RecoveryPointByBackupVault> getRecoveryPoints(String backupVaultName){

        TreeMap<Instant, RecoveryPointByBackupVault> output = 
        new TreeMap<Instant, RecoveryPointByBackupVault>();
        
        //call and response with amazon to get list of vault backups
        ListRecoveryPointsByBackupVaultRequest  request = 
        ListRecoveryPointsByBackupVaultRequest.builder().
        backupVaultName(backupVaultName).build();
        
        ListRecoveryPointsByBackupVaultResponse response = 
        client.listRecoveryPointsByBackupVault(request); 

        for(software.amazon.awssdk.services.backup.model.RecoveryPointByBackupVault r: 
        response.recoveryPoints()){

            output.put(r.completionDate(), r); 
        }

        return output; 
    }

    /**
     * Return most recent recovery point from vault. 
     * @return
     * @throws Exception
     */

    public RecoveryPointByBackupVault getRecentRecoveryPoint(int recoveryNumber) throws Exception{

        if (recoveryNumber > recoveryPoints.size()){

            throw new Exception("Recovery Points Exhausted"); 

        }
        
        return recoveryPoints.get(recoveryPoints.keySet().toArray()[recoveryNumber]); 
    }

    /**
     * Given a recovery point, the function initates the recovery given a recovery point. 
     * @param recoveryPoint
     * @return
     * @throws Exception
     */
    public String startRestore(int recoveryNumber) throws Exception{
        
        RecoveryPointByBackupVault recoveryPoint = getRecentRecoveryPoint(recoveryNumber);
        Map<String, String> raw = getRecoveryMetaData(recoveryPoint);
        Map<String, String> metadata = editRecoveryMeta(raw);
        
        StartRestoreJobRequest request = StartRestoreJobRequest.builder().
        recoveryPointArn(recoveryPoint.recoveryPointArn()).iamRoleArn(recoveryPoint.iamRoleArn())
        .metadata(metadata).build();
        
        StartRestoreJobResponse response = client.startRestoreJob(request); 

        return response.restoreJobId();

    }

    /**
     * Gets meta data of recovery point for restore job request. 
     * @param recoveryPoint
     * @return
     */
    public  Map<String, String> getRecoveryMetaData(RecoveryPointByBackupVault recoveryPoint){

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

    public Map<String, String> editRecoveryMeta(Map<String, String> metaData){

        Map<String, String> output = new HashMap<String, String>(); 

        for(Entry<String, String> entry: metaData.entrySet()){

            if(entry.getKey() != "NetworkInterfaces"){

                if(entry.getKey() == "CpuOptions"){continue;}

                output.put(entry.getKey(), entry.getValue()); 
            }

        }

        return output; 
    }

    public String restoreEC2FromBackup() throws Exception{

        String restoreJobId = startRestore(recoveryNumber); 

        int attempts = 0; 
        String resourceARN = ""; 
        while(attempts < 10){
          
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

        return getInstanceId(resourceARN); 

    }

    public String getInstanceId(String resourceARN){

        Pattern pattern = Pattern.compile("i-\\w+");
        Matcher matcher = pattern.matcher(resourceARN.toString()); 
        String instanceId = ""; 
        
        if(matcher.find()){instanceId = matcher.group();}

        return instanceId; 
    }

}