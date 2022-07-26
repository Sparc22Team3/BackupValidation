package sparc.team3.validator.restore;

import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * Class used to restore the most recent S3 recovery point from "s3sparcvault"
 */
public class S3Restore {

    private final BackupClient client;
    private String restoreBucketName;
    private final TreeMap<Instant, RecoveryPointByBackupVault> recoveryPoints;

    public S3Restore(BackupClient client, String backupVaultName){

        this.client = client;
        this.recoveryPoints = getRecoveryPoints(backupVaultName);

    }

    /**
     * Method gets a list of backup restore points from backup vault and populates a sorted data structure. 
     * @param backupVaultName a string of the name of the backup vault
     * @return a TreeMap of the restore points
     * 
     */
    public TreeMap<Instant, RecoveryPointByBackupVault> getRecoveryPoints(String backupVaultName){

        TreeMap<Instant, RecoveryPointByBackupVault> output = new TreeMap<Instant, RecoveryPointByBackupVault>();
        
        //call and response with amazon to get list of vault backups
        ListRecoveryPointsByBackupVaultRequest  request = ListRecoveryPointsByBackupVaultRequest.builder().
        backupVaultName(backupVaultName).build();
        
        ListRecoveryPointsByBackupVaultResponse response = client.listRecoveryPointsByBackupVault(request); 

        for(software.amazon.awssdk.services.backup.model.RecoveryPointByBackupVault r: response.recoveryPoints()){

            output.put(r.completionDate(), r); 
        }

        return output; 
    }

    /**
     * Return most recent recovery point from vault. 
     * @return the most recent RecoveryPointByBackupVault
     * @throws Exception when there are no recovery points remaining
     */

    public RecoveryPointByBackupVault getRecentRecoveryPoint(int recoveryNumber) throws Exception{

        if (recoveryNumber > recoveryPoints.size()){

            throw new Exception("Recovery Points Exhausted"); 

        }

        // System.out.println("S3 Backup Recovery Point: " + recoveryPoint.toString());

        return recoveryPoints.get(recoveryPoints.keySet().toArray()[recoveryNumber]);
    }

    /**
     * Given a recovery point number, initiates a S3 bucket restore.
     * @param recoveryNumber an int of the recovery point to restore
     * @return Restore Job ID
     * @throws Exception when there is a problem restoring the bucket
     */
    public String restoreS3Resource(int recoveryNumber) throws Exception
    {
        RecoveryPointByBackupVault recoveryPoint = getRecentRecoveryPoint(recoveryNumber);
        Map<String, String> metadata = s3RecoveryMetaData();

        StartRestoreJobRequest request = StartRestoreJobRequest.builder().
        recoveryPointArn(recoveryPoint.recoveryPointArn()).iamRoleArn(recoveryPoint.iamRoleArn())
        .metadata(metadata).build();

        StartRestoreJobResponse response = client.startRestoreJob(request);

        return response.restoreJobId();
    }

    /**
     * Populates a Map with metadata required for S3 restore
     * @return a Map of the string meta data
     */
    public Map<String, String> s3RecoveryMetaData()
    {
        Map<String, String> output = new HashMap<>();

        // The destination bucket for your restore
        restoreBucketName = "bucket" + System.currentTimeMillis();
        output.put("DestinationBucketName", restoreBucketName);

        // Boolean to indicate whether to create a new bucket
        output.put("NewBucket", "true");

        // Boolean to indicate whether to encrypt the restored data
        output.put("Encrypted", "true");

        //? CreationToken: An idempotency token
        output.put("CreationToken", "");

        // EncryptionType: The type of encryption to encrypt your restored objects. Options are original (same encryption as the original object), SSE-S3, or SSE-KMS).
        output.put("EncryptionType", "original");

        return output;
    }

    /**
     * Returns the name of the newly created S3 bucket name
     * @return a string of the restored bucket name
     */
    public String getRestoreBucketName() {
        return restoreBucketName;
    }
}