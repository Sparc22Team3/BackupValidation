import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

//EC2 BackupId a765a264-6cba-4c2d-a56c-42ec0d7abad3
//RDS BackupID 6e36f3fa-d2e0-4afe-8f7f-7ab348f2851b
//S3 BackupID 8A387580-B50A-4F9A-6CF9-436FE67F96C5

public class App {

  public static void main(String[] args) throws IOException {

    try{

      Region region = Region.US_EAST_1;
      BackupClient client =  BackupClient.builder().region(region).build();

      S3Restore s3Restore = new S3Restore(client, "s3sparcvault"); 

      String restoreJobId = s3Restore.restoreS3Resource(0);

      System.out.println("Starting restore job: " + restoreJobId);

      //Wait for restore to provide ID of EC2 instance it created
      int attempts = 0; 
      String resourceARN = "NULL"; 
      while(attempts < 5){

        try{

          //get restore job information and wait until status of restore job is "completed"
          DescribeRestoreJobRequest newRequest = DescribeRestoreJobRequest.builder().restoreJobId(restoreJobId).build(); 
          DescribeRestoreJobResponse restoreResult = client.describeRestoreJob(newRequest);

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
      
            System.out.println("Creating S3 Instance With ID: " + instanceId); 

            break; 
          }

          
        } catch(Exception e){

          System.err.println(e); 

          System.exit(1); 

        }
        Thread.sleep(500000); // Takes about 8min for an S3 bucket to be restored
        attempts++; 
      }

      client.close(); 

   } catch(BackupException e){

      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1); 
      
   } catch (Exception e) {

     System.err.println(e); 

     System.exit(1); 

  }
  
  }

}