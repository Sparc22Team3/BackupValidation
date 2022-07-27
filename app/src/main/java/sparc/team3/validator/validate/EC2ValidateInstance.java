package sparc.team3.validator.validate;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class EC2ValidateInstance {
    private final Instance instance;
    private final Ec2Client ec2Client;

    public EC2ValidateInstance(Ec2Client ec2Client, Instance instance){
        this.ec2Client = ec2Client;
        this.instance = instance;
    }

    /**
     * Ping url and check status code. 
     * @return boolean whether the instance is pingable
     * @throws Exception when there is a problem pinging the instance
     */
    public Boolean validateWithPing(String entryPoint) throws Exception{
        String url = instance.publicDnsName();

        url = "http://" + url + entryPoint;

        waitForEC2Checks();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        return httpResponse.statusCode() == 200;

    }
    
    /**
     * Busy wait for EC2 to complete setup. 
     * @throws Exception when the instance times out.
     */
    private void waitForEC2Checks() throws Exception{
        
      //Wait for ec2 instance to complete setup
      int attempts = 0;
      while(attempts < 11){

        try{

          DescribeInstanceStatusRequest statusReq = DescribeInstanceStatusRequest
          .builder().instanceIds(instance.instanceId()).build();
          
          DescribeInstanceStatusResponse statusRes = ec2Client.describeInstanceStatus(statusReq); 

          String running = statusRes.instanceStatuses().get(0).instanceState().name().toString();
          String sysPass = statusRes.instanceStatuses().get(0).systemStatus().status().toString();
          String reachPass = statusRes.instanceStatuses().get(0).instanceStatus().status().toString();

          System.out.println("Running: " + running);
          System.out.println("Sys Pass: " + sysPass);
          System.out.println("Reach Pass: "+ reachPass);

    
          if((running.equals("running")) && (sysPass.equals("passed") || sysPass.equals("ok")) && (reachPass.equals("passed") || reachPass.equals("ok"))){
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

      if(attempts >= 11){

        throw new Exception("EC2 Instance Timeout"); 
      }
    }

    /**
     * Terminate ec2Instance attached to client.
     */
    public void terminateEC2Instance(){

        System.out.println("Terminating Instance"); 

        TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder().instanceIds(instance.instanceId()).build();
        ec2Client.terminateInstances(terminateRequest); 

    }
}
