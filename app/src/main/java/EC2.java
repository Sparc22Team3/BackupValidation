import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import sparc.team3.validator.restore.EC2Restore;
import sparc.team3.validator.util.InstanceSettings;
import sparc.team3.validator.validate.EC2ValidateInstance;

public class EC2 {

  public static void main(String[] args) {

    try{

      Region region = Region.US_EAST_1;
      // BackupClient client =  BackupClient.builder().region(region).build();
      // int recoveryAttempt = 0;
       Ec2Client ec2Client = Ec2Client.builder().region(region).build();
      // InstanceSettings instanceSettings = new InstanceSettings("ec2sparcvault", null, null, null);

      // EC2Restore restore = new EC2Restore(client, ec2Client, instanceSettings, recoveryAttempt);

      // Instance instance = restore.restoreEC2FromBackup();

      DescribeInstancesRequest instanceReq = DescribeInstancesRequest
      .builder().instanceIds("i-082d6b4bcd330277e").build();
      
      DescribeInstancesResponse instanceRep = ec2Client.describeInstances(instanceReq); 

      Instance instance = instanceRep.reservations().get(0).instances().get(0); 

      EC2ValidateInstance validateInstance = new EC2ValidateInstance(ec2Client, instance);

      Map<String,String> functionMap = new HashMap<String, String>(); 

      functionMap.put("Title","SPARC Absit Omen Lexicon");

      Boolean validated = validateInstance.validateWebFunctionality(functionMap, "/wiki/index.php?title=Main_Page");

      System.out.println(validated);

      //validateInstance.terminateEC2Instance();
      
    

      //close connection
      // ec2Client.close();
      // client.close();
      

   } catch(BackupException e){

      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1); 
   } catch (Exception e) {

     System.err.println(e); 

     System.exit(1); 

  }
  
  }

}