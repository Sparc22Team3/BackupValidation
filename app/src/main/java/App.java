import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import software.amazon.awssdk.regions.Region;

import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.backup.model.DescribeRestoreJobRequest;
import software.amazon.awssdk.services.backup.model.DescribeRestoreJobResponse;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectAttributesRequest;
import software.amazon.awssdk.services.s3.model.GetObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectAttributes;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
// import software.amazon.awssdk.services.s3.model.Checksum;

//EC2 BackupId a765a264-6cba-4c2d-a56c-42ec0d7abad3
//RDS BackupID 6e36f3fa-d2e0-4afe-8f7f-7ab348f2851b
//S3 BackupID 8A387580-B50A-4F9A-6CF9-436FE67F96C5

public class App {

  public static void main(String[] args) throws IOException {

    // ask user to provide S3 bucket name and S3 backup vault name

    Scanner scan = new Scanner(System.in);
    System.out.print("Enter S3 Bucket Name: ");
    String s3BucketName = scan.next();
    System.out.print("Enter S3 Backup Vault Name: ");
    String s3BackupVaultName = scan.next();

    // initialize hashmaps to store S3 objects
    HashMap<String, String> s3BucketObjs = new HashMap<>();
    HashMap<String, String> s3RestoredObjs = new HashMap<>();

    // initialize AWS objects
    Region region = Region.US_EAST_1;
    S3Client s3Client = S3Client.builder().region(region).build();
    BackupClient backupClient =  BackupClient.builder().region(region).build();

    try{

      // // initialize ListObjectsRequest
      // ListObjectsRequest listObjects = ListObjectsRequest
      //         .builder()
      //         .bucket(s3BucketName)
      //         .build();

      // ListObjectsResponse res = s3Client.listObjects(listObjects);
      // List<S3Object> objects = res.contents();

      // // retrieve the keys of the S3 objects and add them to s3BucketObjs
      // for (S3Object myValue : objects) {

      //   // initialize AWS object to get checksum value
      //   GetObjectAttributesResponse
      //   objectAttributes = s3Client.getObjectAttributes(GetObjectAttributesRequest.builder().bucket(s3BucketName).key(myValue.key())
      //   .objectAttributes(ObjectAttributes.OBJECT_PARTS, ObjectAttributes.CHECKSUM).build());

      //   // add S3 object key and checksum value to map
      //   s3BucketObjs.put(myValue.key(), objectAttributes.checksum().checksumSHA256());
      // }

      // initialize s3Restore instance
      S3Restore s3Restore = new S3Restore(backupClient, s3Client, s3BucketName, s3BackupVaultName); 

      s3BucketObjs = s3Restore.getS3Objects(s3BucketName, s3Client);

      String restoreJobId = s3Restore.restoreS3Resource(0);

      System.out.println("Starting restore job: " + restoreJobId);

      //Wait for restore to provide ID of S3 instance it created
      int attempts = 0; 
      String resourceARN = "NULL"; 
      while(attempts < 5){

        try{

          //get restore job information and wait until status of restore job is "completed"
          DescribeRestoreJobRequest newRequest = DescribeRestoreJobRequest.builder().restoreJobId(restoreJobId).build(); 
          DescribeRestoreJobResponse restoreResult = backupClient.describeRestoreJob(newRequest);

          System.out.println("Restore Status:" + restoreResult.status().toString()); 
          
          if(restoreResult.status().toString() == "COMPLETED"){
            System.out.println(restoreResult.toString()); 
            resourceARN = restoreResult.createdResourceArn();

            Pattern pattern = Pattern.compile("\\w+\\d+$");
            Matcher matcher = pattern.matcher(resourceARN.toString()); 
            String instanceId = ""; 
            
            if(matcher.find()){
      
              instanceId = matcher.group(); 
      
            }
      
            System.out.println("S3 Bucket " + s3BucketName + " Restored With ID: " + instanceId); 

            // retrieve all objects from restored s3 bucket


          }

          
        } catch(Exception e){

          System.err.println(e); 

          System.exit(1); 

        }
        Thread.sleep(250000); // Takes about 8min for an S3 bucket to be restored
        attempts++; 

        // Print out message is restore is not successful by the 5th try
        if (attempts == 5){
          System.out.println("Restore job timeout. Please check AWS Backup console to check job status.");
        }
      }

      client.close(); 

   } catch(BackupException e){

      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1); 
      
   } catch (Exception e) {

     System.err.println(e); 

     System.exit(1); 

  }



  // application concludes
  System.out.println("Thank you for using our Backup Validation application! Goodbye!");
  scan.close();
  
  }

}