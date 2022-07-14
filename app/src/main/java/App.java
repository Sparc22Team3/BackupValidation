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

public class App {

  public static void main(String[] args) throws IOException {

    try{

      Region region = Region.US_EAST_1;
      BackupClient client =  BackupClient.builder().region(region).build();

      SparcRestore restore = new SparcRestore(client, "ec2sparcvault"); 

      String restoreJobId = restore.restoreResource(0);

      System.out.println("Starting restore job: " + restoreJobId);

      //Wait for restore to provide ID of EC2 instance it created
      int attempts = 0; 
      String resourceARN = "NULL"; 
      while(attempts < 10){
        
        try{

          //get restore job information and wait until status of restore job is "completed"
          DescribeRestoreJobRequest newRequest = DescribeRestoreJobRequest.builder().restoreJobId(restoreJobId).build(); 
          DescribeRestoreJobResponse restoreResult = client.describeRestoreJob(newRequest);

          System.out.println("Restore Status:" + restoreResult.status().toString()); 
          
          if(restoreResult.status().toString() == "COMPLETED"){
            System.out.println(restoreResult.toString()); 
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

      Pattern pattern = Pattern.compile("i-\\w+");
      Matcher matcher = pattern.matcher(resourceARN.toString()); 
      String instanceId = ""; 
      
      if(matcher.find()){

        instanceId = matcher.group(); 

      }

      System.out.println("Creating EC2 Instance With ID: " + instanceId); 
      
      attempts = 0;
      Ec2Client ec2Client = Ec2Client.builder().region(region).build();

      while(attempts < 10){

        try{

          DescribeInstanceStatusRequest statusReq = DescribeInstanceStatusRequest.builder().instanceIds(instanceId).build();
          DescribeInstanceStatusResponse statusRes = ec2Client.describeInstanceStatus(statusReq); 

          String running = statusRes.instanceStatuses().get(0).instanceState().name().toString();
          String sysPass = statusRes.instanceStatuses().get(0).systemStatus().status().toString();
          String reachPass = statusRes.instanceStatuses().get(0).instanceStatus().status().toString();

          System.out.println("Running: " + running);
          System.out.println("Sys Pass: " + sysPass);
          System.out.println("Reach Pass: "+ reachPass);

    
          if((running == "running") && (sysPass == "passed" || sysPass == "ok" ) && (reachPass == "passed" ||reachPass == "ok")){
            break;
          }

        }

        catch(Exception e){
          
          System.err.println(e);
          System.exit(1); 
        }

        Thread.sleep(60000); 
        attempts++; 
      }

      //Get network information of instance
      DescribeInstancesRequest instanceReq = DescribeInstancesRequest.builder().instanceIds(instanceId).build();
      DescribeInstancesResponse instanceRep = ec2Client.describeInstances(instanceReq); 

      System.out.println(instanceRep.reservations().get(0).instances().get(0).publicDnsName());

      String publicIVP4 = instanceRep.reservations().get(0).instances().get(0).publicDnsName();

      publicIVP4 = "http://" + publicIVP4 + "/wiki/index.php?title=Main_Page";

      HttpClient httpClient = HttpClient.newHttpClient();

      HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(publicIVP4)).build(); 

      HttpResponse httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()); 

      System.out.println(httpResponse.toString()); 

      //close connection
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