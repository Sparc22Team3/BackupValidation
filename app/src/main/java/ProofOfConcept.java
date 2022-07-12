import java.io.IOException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.backup.model.ListRecoveryPointsByBackupVaultRequest;
import software.amazon.awssdk.services.backup.model.ListRecoveryPointsByBackupVaultResponse;


//BACKUP Plan IDs

//EC2 BackupId a765a264-6cba-4c2d-a56c-42ec0d7abad3
//RDS BackupID 6e36f3fa-d2e0-4afe-8f7f-7ab348f2851b

public class ProofOfConcept {

    public static void main(String[] args) throws IOException {

        try{
            System.out.println("Main Starting");
            System.out.printf("%n");

            Region region = Region.US_EAST_1;
            BackupClient client =  BackupClient.builder().region(region).build();

            //Build request object
            ListRecoveryPointsByBackupVaultRequest  request = ListRecoveryPointsByBackupVaultRequest.builder().backupVaultName("ec2sparcvault").build();

            //build response object
            ListRecoveryPointsByBackupVaultResponse response = client.listRecoveryPointsByBackupVault(request);

            System.out.println("Printing list of size:" + response.recoveryPoints().size());
            System.out.printf("%n");

            //Iterate through the response backup selections list, print
            for(software.amazon.awssdk.services.backup.model.RecoveryPointByBackupVault r: response.recoveryPoints()){

                System.out.println(r.toString());
                System.out.printf("%n");

            }

            //close connection
            client.close();
        } catch(BackupException e){

            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }



    }

    // public static void tutorialSetup(S3Client s3Client, String bucketName, Region region) {
    //   try {
    //     s3Client.createBucket(CreateBucketRequest
    //         .builder()
    //         .bucket(bucketName)
    //         .createBucketConfiguration(
    //             CreateBucketConfiguration.builder()
    //                 .locationConstraint(region.id())
    //                 .build())
    //         .build());
    //     System.out.println("Creating bucket: " + bucketName);
    //     s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder()
    //         .bucket(bucketName)
    //         .build());
    //     System.out.println(bucketName +" is ready.");
    //     System.out.printf("%n");
    //   } catch (S3Exception e) {
    //     System.err.println(e.awsErrorDetails().errorMessage());
    //     System.exit(1);
    //   }
    // }

    // public static void cleanUp(S3Client s3Client, String bucketName, String keyName) {
    //   System.out.println("Cleaning up...");
    //   try {
    //     System.out.println("Deleting object: " + keyName);
    //     DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(keyName).build();
    //     s3Client.deleteObject(deleteObjectRequest);
    //     System.out.println(keyName +" has been deleted.");
    //     System.out.println("Deleting bucket: " + bucketName);
    //     DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
    //     s3Client.deleteBucket(deleteBucketRequest);
    //     System.out.println(bucketName +" has been deleted.");
    //     System.out.printf("%n");
    //   } catch (S3Exception e) {
    //     System.err.println(e.awsErrorDetails().errorMessage());
    //     System.exit(1);
    //   }
    //   System.out.println("Cleanup complete");
    //   System.out.printf("%n");
    // }
}