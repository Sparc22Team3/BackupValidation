package com.example.myapp;

import java.io.IOException;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupException;

//BACKUP Plan IDs

//EC2 BackupId a765a264-6cba-4c2d-a56c-42ec0d7abad3
//RDS BackupID 6e36f3fa-d2e0-4afe-8f7f-7ab348f2851b

public class App {

  public static void main(String[] args) throws IOException {

    try{

      Region region = Region.US_EAST_1;
      BackupClient client =  BackupClient.builder().region(region).build();

      SparcRestore restore = new SparcRestore(client, "ec2sparcvault"); 

      restore.restoreResource(0);

      //close connection
      client.close(); 


   } catch(BackupException e){

      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1); 
   } catch (Exception e) {

     System.err.println("Recovery Points Exhausted"); 
     System.exit(1); 

  }
  
  }

}