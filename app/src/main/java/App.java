//package com.example.myapp;

import java.io.IOException;

//import com.example.rds.RDSRestore;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.RestoreDbInstanceFromDbSnapshotRequest;

//BACKUP Plan IDs

//EC2 BackupId a765a264-6cba-4c2d-a56c-42ec0d7abad3
//RDS BackupID 6e36f3fa-d2e0-4afe-8f7f-7ab348f2851b

public class App {

    public static void main(String[] args) throws IOException {

        try{

/**            Region region = Region.US_EAST_1;
            BackupClient client =  BackupClient.builder()
                    .region(region).build();

            SparcRestore restore = new SparcRestore(client, "ec2sparcvault");

            restore.restoreResource(0);

            //close connection
            client.close();
 */

            Region region = Region.US_EAST_1;
            RdsClient rdsClient = RdsClient.builder()
                    .region(region)
                  //  .credentialsProvider(ProfileCredentialsProvider.create())
                    .build();

            RDSRestore rdsRestore = new RDSRestore(rdsClient, "rdssparcvault");

            // rdsRestore.describeSnapshots(rdsClient);
            // rdsRestore.restoreResource("arn:aws:rds:us-east-1:490610433117:snapshot:awsbackup:job-685723b2-f0b1-b990-7428-5cb3f804a7d6");
             rdsRestore.restoreResource();

            rdsClient.close();


        } catch(BackupException e){

            System.err.println(e.awsErrorDetails().errorMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {

            System.err.println("Recovery Points Exhausted");
            e.printStackTrace();
            System.exit(1);

        }

    }

}

