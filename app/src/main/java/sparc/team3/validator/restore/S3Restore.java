package sparc.team3.validator.restore;

import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.backup.model.DescribeRestoreJobRequest;
import software.amazon.awssdk.services.backup.model.DescribeRestoreJobResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import sparc.team3.validator.config.settings.InstanceSettings;
import sparc.team3.validator.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;


/**
 * Class used to restore the most recent S3 recovery point from S3 backup vault
 */
public class S3Restore extends AWSRestore implements Callable<String> {
    private String restoreBucketName;
    private final S3Client s3Client;

    public S3Restore(BackupClient backupClient, S3Client s3Client, InstanceSettings instanceSettings){
        super(backupClient, instanceSettings, "S3");
        this.s3Client = s3Client;
    }

    @Override
    public String call() throws InterruptedException, RecoveryPointsExhaustedException {
        return restoreS3FromBackup();
    }

    /**
     * Polls AWS Backup to check when restore job is complete. Returns error if restore job took
     * longer than 20 minutes.
     * If restore job is successful, copy bucket policy from production bucket to restored bucket.
     * Throws error if job isn't completed within allotted time.
     * @return a string of the bucket name
     * @throws InterruptedException when sleep is interrupted
     */
    public String restoreS3FromBackup() throws InterruptedException, RecoveryPointsExhaustedException {

        String restoreJobId = startRestore();
        if(restoreJobId == null)
            return null;
        int sleepMinutes = 4;
        String finalStatus = null;

        logger.info("Restore S3 job: {}", restoreJobId);
        while (finalStatus == null) {
            try {
                //get restore job information and wait until status of restore job is "completed"
                DescribeRestoreJobRequest newRequest = DescribeRestoreJobRequest
                        .builder().restoreJobId(restoreJobId).build();
                DescribeRestoreJobResponse restoreResult = backupClient.describeRestoreJob(newRequest);
                String status = restoreResult.status().toString();

                logger.info("Restore Job {}\t\tStatus: {}\t\tPercent Done: {}", restoreJobId,
                        status, restoreResult.percentDone());

                // when restore job is no longer running, break out of the loop otherwise sleep for a while
                if (!status.equals("RUNNING") && !status.equals("PENDING")) {
                    finalStatus = status;
                } else {
                    Thread.sleep(sleepMinutes * 60000);
                }
            } catch (BackupException e) {
                logger.error("Problem with restore job id: {}", restoreJobId, e);
                return null;
            }
        }

        if (!finalStatus.equals("COMPLETED")){
            logger.error("Restore job {} did not complete.", restoreJobId);
            return null;
        }

        // copy bucket policy from production bucket to restored bucket
        copyS3Policy();

        return restoreBucketName;
    }

    /**
     * Populates a Map with metadata required for S3 restore
     * @return a Map of the string metadata
     */
    Map<String, String> setMetadata()
    {
        Map<String, String> output = new HashMap<>();

        // The destination bucket for your restore
        restoreBucketName = Util.UNIQUE_RESTORE_NAME_BASE + System.currentTimeMillis();
        output.put("DestinationBucketName", restoreBucketName);

        // Boolean to indicate whether to create a new bucket
        output.put("NewBucket", "true");

        // Boolean to indicate whether to encrypt the restored data
        output.put("Encrypted", "true");

        // CreationToken: An idempotency token
        output.put("CreationToken", "");

        // EncryptionType: The type of encryption to encrypt your restored objects. Options are original (same encryption as the original object), SSE-S3, or SSE-KMS).
        output.put("EncryptionType", "original");

        return output;
    }

    /**
     * Retrieves the policy assigned to the original s3 bucket (production bucket), and switches out the resource name
     * in the policy, and then adding the policy to the restored s3 bucket.
     * The policy is set for s3 objects to have public access.
     */
    private void copyS3Policy(){

        String s3ProductionBucketName = instanceSettings.getProductionName();
        GetBucketPolicyRequest getPolicyReq = GetBucketPolicyRequest.builder().bucket(s3ProductionBucketName).build();

        try {
            // get policy from production bucket
            logger.info("Retrieving policy from bucket: " + s3ProductionBucketName);
            GetBucketPolicyResponse getPolicyRsp = s3Client.getBucketPolicy(getPolicyReq);
            String policyText = getPolicyRsp.policy();

            // replace ARN (bucket name) to that of the restored bucket
            String replacementPolicy = policyText.replace(s3ProductionBucketName, restoreBucketName);

            // put replacementPolicy to restored bucket
            logger.info("Putting policy to bucket: " + restoreBucketName);
            PutBucketPolicyRequest putPolicyReq = PutBucketPolicyRequest.builder().bucket(restoreBucketName).policy(replacementPolicy).build();
            s3Client.putBucketPolicy(putPolicyReq);

        } catch (S3Exception e) {
            logger.error("Error in copying policy from {} to {}", s3ProductionBucketName, restoreBucketName, e);
        }
    }
}