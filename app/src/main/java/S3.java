import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import sparc.team3.validator.restore.S3Restore;
import sparc.team3.validator.util.InstanceSettings;
import sparc.team3.validator.validate.S3Validate;

import java.util.Scanner;


public class S3 {

  public static void main(String[] args) {

    // ask user to provide S3 bucket name and S3 backup vault name

    Scanner scan = new Scanner(System.in);
    System.out.print("Enter S3 Bucket Name: ");
    String s3BucketName = scan.next();
    System.out.print("Enter S3 Backup Vault Name: ");
    String s3BackupVaultName = scan.next();

    // initialize AWS objects
    Region region = Region.US_EAST_1;
    S3Client s3Client = S3Client.builder().region(region).build();
    BackupClient backupClient =  BackupClient.builder().region(region).build();

    try{
      InstanceSettings instanceSettings = new InstanceSettings("sparc-team3-s3bucket", s3BackupVaultName, null, null);

      // restore s3
      S3Restore s3Restore = new S3Restore(backupClient, s3Client, instanceSettings);

      // start with the latest recovery point
      int recoveryNumber = 0;

      //! potential to add loop here to restore later recovery points if first one fails
      String restoreJobId = s3Restore.startRestore(recoveryNumber);

      // get the name of the restored S3 instance to initialize S3 Waiter
      String restoredBucketName = s3Restore.getRestoreBucketName();
      System.out.println("Starting restore job: " + restoreJobId);

      // Wait until the bucket is created and print out the response
      S3Waiter s3Waiter = s3Client.waiter();
      HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
              .bucket(restoredBucketName)
              .build();

      //! could add a try-catch here to catch exception that the restore did not complete
      WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
      waiterResponse.matched().response().ifPresent(System.out::println);
      System.out.println("S3 Bucket " + s3BucketName + " Restored to new Bucket: " + restoredBucketName);

      // initialize sparc.team3.validator.validate.S3Validate object
      S3Validate s3Validate = new S3Validate(s3Client, s3BucketName, restoredBucketName);

      // checksum validation
      boolean checksumCheck = s3Validate.ChecksumValidate();
      if (checksumCheck) {
        System.out.println("S3 Restore successfully validated!");
      } else {
        System.out.println("S3 Restore validation failed.");
      }

//      //Wait for restore to provide ID of S3 instance it created
//      int attempts = 0;
//      String resourceARN = "NULL";
//      while(attempts < 5){
//
//        try{
//
//          //get restore job information and wait until status of restore job is "completed"
//          DescribeRestoreJobRequest newRequest = DescribeRestoreJobRequest.builder().restoreJobId(restoreJobId).build();
//          DescribeRestoreJobResponse restoreResult = backupClient.describeRestoreJob(newRequest);
//
//          System.out.println("Restore Status:" + restoreResult.status().toString());
//
//          if(Objects.equals(restoreResult.status().toString(), "COMPLETED")){
//            System.out.println(restoreResult.toString());
//            resourceARN = restoreResult.createdResourceArn();
//
//            Pattern pattern = Pattern.compile("\\w+\\d+$");
//            Matcher matcher = pattern.matcher(resourceARN.toString());
//            String restoredBucket = "";
//
//            // obtain the restored s3 bucket name using regex
//            if(matcher.find()){
//              restoredBucket = matcher.group();
//            }
//
//            System.out.println("S3 Bucket " + s3BucketName + " Restored to new Bucket: " + restoredBucket);
//
//            // initialize sparc.team3.validator.validate.S3Validate object
//            sparc.team3.validator.validate.S3Validate s3Validate = new sparc.team3.validator.validate.S3Validate(s3Client, s3BucketName, restoredBucket);
//
//            // checksum validation
//            boolean checksumCheck = s3Validate.ChecksumValidate();
//
//            if (checksumCheck) {
//              System.out.println("S3 Restore successfully validated!");
//            } else {
//              System.out.println("S3 Restore validation failed.");
//            }
//
//          }
//
//        } catch(Exception e){
//
//          System.err.println(e);
//
//          System.exit(1);
//
//        }
//        Thread.sleep(250000); // Takes about 8min for an S3 bucket to be restored
//        attempts++;
//
//        // Print out message is restore is not successful by the 5th try
//        if (attempts == 5){
//          System.out.println("Restore job timeout. Please check AWS Backup console to check job status.");
//        }
//      }

      backupClient.close();
      s3Client.close();

   }  catch (S3Exception | BackupException e) {

      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);

   } catch (Exception e) {

     System.err.println(e);
     System.exit(1); 

  }

  // application concludes
  System.out.println("Thank you for using our Backup Validation application. Goodbye!");
  scan.close();

  }

}