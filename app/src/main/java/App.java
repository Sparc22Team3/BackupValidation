//package com.example.myapp;

import java.io.IOException;

//import com.example.rds.RDSRestore;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBSnapshot;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.RestoreDbInstanceFromDbSnapshotRequest;

//BACKUP Plan IDs

//EC2 BackupId a765a264-6cba-4c2d-a56c-42ec0d7abad3
//RDS BackupID 6e36f3fa-d2e0-4afe-8f7f-7ab348f2851b

public class App {

    public static void main(String[] args) throws IOException {

        try{


            Region region = Region.US_EAST_1;
            String rdsSparcVault = "rdssparcvault";
            RdsClient rdsClient = RdsClient.builder()
                    .region(region)
                    .build();

            RDSRestore rdsRestore = new RDSRestore(rdsClient, rdsSparcVault);
            // rdsRestore.describeSnapshots(); //just for testing
            rdsRestore.restoreResource();

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

