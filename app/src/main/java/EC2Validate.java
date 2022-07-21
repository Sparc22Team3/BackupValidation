import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

public class EC2Validate {

    String instanceId; 
    Ec2Client ec2Client; 
    DescribeInstancesResponse description; 

    public EC2Validate(Ec2Client client, String instance){
        instanceId = instance; 
        ec2Client = client; 
        description = getInstanceDescription(instance);
    }

    /**
     * Return the medata data for the instance
     * @param instance
     * @return
     */
    DescribeInstancesResponse getInstanceDescription(String instance){

        DescribeInstancesRequest instanceReq = DescribeInstancesRequest
        .builder().instanceIds(instanceId).build();
        
        DescribeInstancesResponse instanceRep = ec2Client.describeInstances(instanceReq); 

        return instanceRep;
    }

    /**
     * Given instance meta data, return formated url. 
     * @param instanceRep
     * @return
     */
    String getInstancePublicURL(DescribeInstancesResponse instanceRep){

        String url = instanceRep.reservations().get(0).instances().get(0).publicDnsName();
        
        url = "http://" + url + "/wiki/index.php?title=Main_Page";

        return url;
    }

    /**
     * Ping url and check status code. 
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    Boolean validateWithPing() throws IOException, InterruptedException{

        String url = getInstancePublicURL(description); 
        waitForEC2Checks();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(url)).build(); 
        HttpResponse httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if(httpResponse.statusCode() == 200){

            terminateEC2Instance();

            return true;

        }

        terminateEC2Instance();

        return false; 

    }

    public void waitForEC2Checks() throws InterruptedException{
        
      //Wait for ec2 instance to complete setup
      int attempts = 0;
      while(attempts < 10){

        try{

          DescribeInstanceStatusRequest statusReq = DescribeInstanceStatusRequest
          .builder().instanceIds(instanceId).build();
          
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
    }

    public void terminateEC2Instance(){

        System.out.println("Terminating Instance"); 

        TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder().instanceIds(instanceId).build(); 
        ec2Client.terminateInstances(terminateRequest); 

    }
    
}
