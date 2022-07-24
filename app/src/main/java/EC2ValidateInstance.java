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

public class EC2ValidateInstance {

    private String instanceARN; 
    private String instanceId; 
    private Ec2Client ec2Client; 
    private DescribeInstancesResponse description; 

    private String publicDNS; 
    private String publicIP;
    private String privateIP; 
    private String subnet;

    public EC2ValidateInstance(Ec2Client client, String instance, String resourceARN){
        instanceARN = resourceARN; 
        instanceId = instance; 
        ec2Client = client; 
        description = getInstanceDescription(instance);
        publicDNS = getInstancePublicDNS(description); 
        publicIP = description.reservations().get(0).instances().get(0).publicIpAddress();
        privateIP = description.reservations().get(0).instances().get(0).privateIpAddress();
        subnet = description.reservations().get(0).instances().get(0).subnetId();
    }

    /**
     * Return the medata data for the instance
     * @param instance
     * @return
     */
    private DescribeInstancesResponse getInstanceDescription(String instance){

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
    private String getInstancePublicDNS(DescribeInstancesResponse instanceRep){

        String url = instanceRep.reservations().get(0).instances().get(0).publicDnsName();
        
        url = "http://" + url + "/wiki/index.php?title=Main_Page";

        return url;
    }

    /**
     * Ping url and check status code. 
     * @return
     * @throws Exception
     */
    public Boolean validateWithPing() throws Exception{

        waitForEC2Checks();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(publicDNS)).build(); 
        HttpResponse httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if(httpResponse.statusCode() == 200){

            return true;

        }

        return false; 

    }
    
    /**
     * Busy wait for EC2 to complete setup. 
     * @throws Exception
     */
    private void waitForEC2Checks() throws Exception{
        
      //Wait for ec2 instance to complete setup
      int attempts = 0;
      while(attempts < 11){

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

      if(attempts >= 11){

        throw new Exception("EC2 Instance Timeout"); 
      }
    }

    /**
     * Terminate ec2Instance attached to client.
     */
    public void terminateEC2Instance(){

        System.out.println("Terminating Instance"); 

        TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder().instanceIds(instanceId).build(); 
        ec2Client.terminateInstances(terminateRequest); 

    }
    
    /**
     * Provide class with a new instanceId to reset description and url. 
     */
    public void setInstanceId(String instanceId){

      this.instanceId = instanceId; 
      
      description = getInstanceDescription(this.instanceId);
      publicDNS = getInstancePublicDNS(description); 
    }

    public String getPublicIP(){

      return publicIP;

    }

    public String getPrivateIP(){

      return privateIP;
    }

    public String getSubnet(){
      return subnet;
    }

    public String getInstanceARN(){
      return instanceARN;
    }
}
