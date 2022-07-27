//package com.example.myapp;

import java.io.IOException;

//import com.example.rds.RDSRestore;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.rds.RdsClient;

//BACKUP Plan IDs

//EC2 BackupId a765a264-6cba-4c2d-a56c-42ec0d7abad3
//RDS BackupID 6e36f3fa-d2e0-4afe-8f7f-7ab348f2851b

/**
 * Runs the restore, test, and validate of an RDS snapshot.
 */
public class App {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws IOException the io exception
     */
    public static void main(String[] args) throws IOException {

        try{

            Region region = Region.US_EAST_1;
            String uniqueNameForRestoredDBInstance = "database-test" +System.currentTimeMillis(); //put in util
            String rdsSparcVault = "rdssparcvault";
            String subnetGroupName = "team3-sparc-db-subnet-group"; //add to settings (or a security class?)
            String securityGroupID = "sg-078715763233fad97"; //add to settings (or a security class?)
            RdsClient rdsClient = RdsClient
                    .builder()
                    .region(region)
                    .build();

            RDSRestore rdsRestore
                     = new RDSRestore(rdsClient, uniqueNameForRestoredDBInstance, rdsSparcVault, subnetGroupName, securityGroupID);

            String restoredInstanceID = rdsRestore.restoreResource();

            RDSValidate rdsValidate = new RDSValidate(rdsClient, restoredInstanceID);
            rdsValidate.validateResource(restoredInstanceID);

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

