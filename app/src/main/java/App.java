import java.io.IOException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.ec2.Ec2Client;

//EC2 BackupId a765a264-6cba-4c2d-a56c-42ec0d7abad3
//RDS BackupID 6e36f3fa-d2e0-4afe-8f7f-7ab348f2851b

public class App {

  public static void main(String[] args) throws IOException {

    try{

      Region region = Region.US_EAST_1;
      BackupClient client =  BackupClient.builder().region(region).build();

      EC2Restore restore = new EC2Restore(client, "ec2sparcvault", 0); 

      String instanceId = restore.restoreEC2FromBackup();
      Ec2Client ec2Client = Ec2Client.builder().region(region).build(); 

      EC2Validate instance = new EC2Validate(ec2Client, instanceId);

      Boolean validated = instance.validateWithPing();

      System.out.println("Web Server Status 200: " + validated); 

      //close connection
      ec2Client.close();
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