//package com.example.myapp;

import java.io.IOException;

//import com.example.rds.RDSRestore;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;

//BACKUP Plan IDs

//EC2 BackupId a765a264-6cba-4c2d-a56c-42ec0d7abad3
//RDS BackupID 6e36f3fa-d2e0-4afe-8f7f-7ab348f2851b

public class App {

    public static void main(String[] args) throws IOException {

        try{

            Region region = Region.US_EAST_1;
            String uniqueNameForRestoredDBInstance = "database-test" +System.currentTimeMillis(); //put in util
            String rdsSparcVault = "rdssparcvault";
            String subnetGroupName = "team3-sparc-db-subnet-group"; //add to settings (or a security class?)
            RdsClient rdsClient = RdsClient
                    .builder()
                    .region(region)
                    .build();

             RDSRestore rdsRestore = new RDSRestore(rdsClient, uniqueNameForRestoredDBInstance, rdsSparcVault, subnetGroupName);

            // rdsRestore.describeSnapshots(); //just for testing
            String instanceIdentifier = rdsRestore.restoreResource();
            System.out.println("in main: " +instanceIdentifier);

            //RdsValidate rdsValidate = new RdsValidate(restoredRDS, instanceIdentifier);

             rdsClient.close();


        } catch(BackupException e){

            System.err.println(e.awsErrorDetails().errorMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {

            e.printStackTrace();
            System.exit(1);

        }

    }

}

