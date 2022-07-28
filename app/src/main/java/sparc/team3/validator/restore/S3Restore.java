package sparc.team3.validator.restore;

import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import sparc.team3.validator.util.InstanceSettings;
import sparc.team3.validator.util.Util;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * Class used to restore the most recent S3 recovery point from S3 backup vault
 */
public class S3Restore {

    private final BackupClient backupClient;
    private String restoreBucketName;
    private final S3Client s3Client;
    private final TreeMap<Instant, RecoveryPointByBackupVault> recoveryPoints;

    public S3Restore(BackupClient backupClient, S3Client s3Client, InstanceSettings instanceSettings){
        this.backupClient = backupClient;
        this.s3Client = s3Client;
        this.recoveryPoints = getRecoveryPoints(instanceSettings.getBackupVault());
    }

    /**
     * Method gets a list of backup restore points from backup vault and populates a sorted data structure.
     * @param backupVaultName a string of the name of the backup vault
     * @return a TreeMap of the restore points
     *
     */
    private TreeMap<Instant, RecoveryPointByBackupVault> getRecoveryPoints(String backupVaultName){

        TreeMap<Instant, RecoveryPointByBackupVault> output = new TreeMap<>();

        //call and response with amazon to get list of vault backups
        ListRecoveryPointsByBackupVaultRequest  request = ListRecoveryPointsByBackupVaultRequest.builder().
        backupVaultName(backupVaultName).build();

        ListRecoveryPointsByBackupVaultResponse response = backupClient.listRecoveryPointsByBackupVault(request);

        for(RecoveryPointByBackupVault r: response.recoveryPoints()){
            output.put(r.completionDate(), r);
        }

        return output;
    }

    /**
     * Return most recent recovery point from vault.
     * @return the most recent RecoveryPointByBackupVault
     * @throws Exception when there are no recovery points remaining
     */

    private RecoveryPointByBackupVault getRecentRecoveryPoint(int recoveryNumber) throws Exception{

        if (recoveryNumber > recoveryPoints.size()){

            throw new Exception("Recovery Points Exhausted");

        }

        return recoveryPoints.get(recoveryPoints.keySet().toArray()[recoveryNumber]);
    }

    /**
     * Given a recovery point number, initiates a S3 bucket restore.
     * @param recoveryNumber an int of the recovery point to restore
     * @return Restore Job ID
     * @throws Exception when there is a problem restoring the bucket
     */
    private String startRestore(int recoveryNumber) throws Exception
    {
        RecoveryPointByBackupVault recoveryPoint = getRecentRecoveryPoint(recoveryNumber);
        Map<String, String> metadata = s3RecoveryMetaData();

        StartRestoreJobRequest request = StartRestoreJobRequest.builder().
        recoveryPointArn(recoveryPoint.recoveryPointArn()).iamRoleArn(recoveryPoint.iamRoleArn())
        .metadata(metadata).build();

        StartRestoreJobResponse response = backupClient.startRestoreJob(request);

        return response.restoreJobId();
    }

    /**
     * Polls AWS Backup to check when restore job is complete. Returns error if restore job took
     * longer than 20 minutes.
     * Throws error if job isn't completed within allotted time.
     * @return a string of the bucket name
     * @throws Exception when the backup restore times out
     */
    public String restoreS3FromBackup(int recoveryNumber) throws Exception{

        String restoreJobId = startRestore(recoveryNumber);
        System.out.println("Starting restore job: " + restoreJobId);
        System.out.println("Please wait patiently as an S3 bucket restore takes about 20min...");

        int attempts = 0;
        while (attempts < 6) {

            try {
                //get restore job information and wait until status of restore job is "completed"
                DescribeRestoreJobRequest newRequest = DescribeRestoreJobRequest
                        .builder().restoreJobId(restoreJobId).build();
                DescribeRestoreJobResponse restoreResult = backupClient.describeRestoreJob(newRequest);

                System.out.println("Restore Status:" + restoreResult.status().toString());

                // when restore job is completed, break out of the loop
                if (restoreResult.status().toString().equals("COMPLETED")) {
                    System.out.println("Restore job COMPLETED!");
                    break;
                }

            } catch (Exception e) {
                System.err.println(e);
                System.exit(1);
            }

            // loop takes into account the avg time it takes for S3 bucket to complete restore
            // each loop sleeps for 3 minutes, 3 * 6 = 18min
            Thread.sleep(180000);
            attempts++;
        }

        // Print out message if restore is not completed by the 7th try
        if (attempts >= 6) {
            throw new Exception("Backup Restore Job Timeout");
        }

        return restoreBucketName;

    }

    /**
     * Populates a Map with metadata required for S3 restore
     * @return a Map of the string metadata
     */
    private Map<String, String> s3RecoveryMetaData()
    {
        Map<String, String> output = new HashMap<>();

        // The destination bucket for your restore
        restoreBucketName = Util.UNIQUE_RESTORE_NAME_BASE + System.currentTimeMillis();
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

}