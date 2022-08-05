import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import sparc.team3.validator.restore.S3Restore;
import sparc.team3.validator.util.InstanceSettings;
import sparc.team3.validator.util.Util;
import sparc.team3.validator.validate.S3ValidateBucket;

import java.util.Scanner;


public class S3 {

  public static void main(String[] args) {

    // ask user to provide S3 bucket name and S3 backup vault name
    Scanner scan = new Scanner(System.in);
    System.out.print("Enter S3 Bucket Name: ");
    String s3ProductionBucketName = scan.next();
    System.out.print("Enter S3 Backup Vault Name: ");
    String s3BackupVaultName = scan.next();

    // initialize AWS objects
    Region region = Region.US_EAST_1;
    S3Client s3Client = S3Client.builder().region(region).build();
    BackupClient backupClient =  BackupClient.builder().region(region).build();

    try{
      InstanceSettings instanceSettings = new InstanceSettings(s3ProductionBucketName, s3BackupVaultName, null, null);

      // create a S3Restore instance
      S3Restore s3Restore = new S3Restore(backupClient, s3Client, instanceSettings);

      // start restore job
      String restoredBucketName = s3Restore.restoreS3FromBackup();

      // initialize sparc.team3.validator.validate.S3ValidateBucket object
      S3ValidateBucket s3ValidateBucket = new S3ValidateBucket(s3Client, instanceSettings);
      s3ValidateBucket.setRestoredBucket(restoredBucketName);

      // checksum validation
      boolean checksumCheck = s3ValidateBucket.ChecksumValidate();
      if (checksumCheck) {
        System.out.println("S3 AWSRestore successfully validated!");
      } else {
        System.out.println("S3 AWSRestore validation failed.");
      }

      // delete restored instance of S3 bucket
      Util.deleteS3Instance(restoredBucketName, s3Client);

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