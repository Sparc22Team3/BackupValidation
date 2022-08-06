import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import sparc.team3.validator.restore.EC2Restore;
import sparc.team3.validator.util.InstanceSettings;
import sparc.team3.validator.util.selenium.HtmlTag;
import sparc.team3.validator.util.selenium.Login;
import sparc.team3.validator.util.selenium.SearchTerm;
import sparc.team3.validator.util.selenium.SeleniumSettings;
import sparc.team3.validator.validate.EC2ValidateInstance;
import sparc.team3.validator.validate.WebAppValidate;

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
      .builder().instanceIds("i-0f9722e6659a3c917").build();
      
      DescribeInstancesResponse instanceRep = ec2Client.describeInstances(instanceReq); 

      Instance instance = instanceRep.reservations().get(0).instances().get(0); 

      // SeleniumSettings settings = new SeleniumSettings(); 

      // Map<String, String> title = new HashMap<String, String>(); 

      // title.put("/wiki/index.php?title=Main_Page", "SPARC Absit Omen Lexicon");

      // settings.setTitles(title);

      // Set<HtmlTag> tags = new HashSet<HtmlTag>();

      // HtmlTag tag = new HtmlTag("span", "Chapter=", "/wiki/index.php?title=Main_Page");

      // tags.add(tag); 

      // settings.setTags(tags);

      // Set<Login> logins = new HashSet<Login>(); 

      // Login login = new Login();

      // login.setUsername("Sparc");
      // login.setPassword("DinnerCrime");

      // login.setUsernameFieldID("wpName1");
      // login.setPasswordFieldID("wpPassword1");

      // login.setEntrypoint("/wiki/index.php?title=Special:UserLogin&returnto=Main+Page");

      // logins.add(login);

      // settings.setLogins(logins);

      // Set<SearchTerm> terms = new HashSet<SearchTerm>();

      // SearchTerm term = new SearchTerm(); 

      // term.setTerm("");
      // term.setEntrypoint("/wiki/index.php?title=Main_Page");
      // term.setSearchFieldID("searchInput");

      // terms.add(term);
      // settings.setSearchTerms(terms);

      WebAppValidate validateInstance = new WebAppValidate(instance, settings);

      Boolean validated = validateInstance.validateWebFunctionality("/wiki/index.php?title=Main_Page");

      System.out.println(""); 
      System.out.println("System returned:" + validated);

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