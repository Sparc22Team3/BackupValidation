import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import sparc.team3.validator.restore.S3Restore;
import sparc.team3.validator.util.InstanceSettings;
import sparc.team3.validator.validate.S3ValidateBucket;

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

    //---------- TESTING VALIDATION ------------//
//    String s3BucketName = "sparc-s3-team3-test";
//    String s3BackupVaultName = "s3backupvault";
//    String s3RestoredBucketName = "sparc-team3-s3-test2";
//
//    // initialize sparc.team3.validator.validate.S3ValidateBucket object
//    S3ValidateBucket s3Validate1 = new S3ValidateBucket(s3Client, s3BucketName, s3RestoredBucketName);
//
//    // checksum validation
//    boolean checksumCheck1 = s3Validate1.ChecksumValidate();
//    if (checksumCheck1) {
//      System.out.println("S3 AWSRestore successfully validated!");
//    } else {
//      System.out.println("S3 AWSRestore validation failed.");
//    }
//
//    System.exit(0);
    //---------- TESTING VALIDATION ------------//

    try{
      InstanceSettings instanceSettings = new InstanceSettings("sparc-team3-s3bucket", s3BackupVaultName, null, null);

      // create a S3Restore instance
      S3Restore s3Restore = new S3Restore(backupClient, s3Client, instanceSettings);

      // use the latest recovery point for restore job - hard-coded for now, no need for user specification
      int recoveryNumber = 0;

      // start restore job
      String restoredBucketName = s3Restore.restoreS3FromBackup(recoveryNumber);

      // initialize sparc.team3.validator.validate.S3ValidateBucket object
      S3ValidateBucket s3ValidateBucket = new S3ValidateBucket(s3Client, s3BucketName, restoredBucketName);

      // checksum validation
      boolean checksumCheck = s3ValidateBucket.ChecksumValidate();
      if (checksumCheck) {
        System.out.println("S3 AWSRestore successfully validated!");
      } else {
        System.out.println("S3 AWSRestore validation failed.");
      }

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