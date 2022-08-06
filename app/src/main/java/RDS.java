//package com.example.myapp;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.BackupException;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import sparc.team3.validator.restore.RDSRestore;
import sparc.team3.validator.util.InstanceSettings;
import sparc.team3.validator.util.SecurityGroup;
import sparc.team3.validator.validate.RDSValidate;

import java.util.LinkedList;

//BACKUP Plan IDs

//EC2 BackupId a765a264-6cba-4c2d-a56c-42ec0d7abad3
//RDS BackupID 6e36f3fa-d2e0-4afe-8f7f-7ab348f2851b

/**
 * Runs the restore, test, and validate of an RDS snapshot.
 */
public class RDS {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {

        try{

            Region region = Region.US_EAST_1;
            String rdsSparcVault = "rdssparcvault";
            String subnetGroupName = "team3-sparc-db-subnet-group"; //add to settings (or a security class?)
            String securityGroupID = "sg-078715763233fad97"; //add to settings (or a security class?)
            SecurityGroup securityGroup = new SecurityGroup(securityGroupID, "VPC-DB-Security-Group");
            LinkedList<SecurityGroup> sgs = new LinkedList<>();
            sgs.add(securityGroup);
            InstanceSettings instanceSettings = new InstanceSettings("database-1", rdsSparcVault, sgs, subnetGroupName);
            RdsClient rdsClient = RdsClient
                    .builder()
                    .region(region)
                    .build();
            BackupClient backupClient = BackupClient.builder().region(region).build();
            RDSRestore rdsRestore
                     = new RDSRestore(backupClient, rdsClient, instanceSettings);

            DBInstance restoredInstance = rdsRestore.restoreRDSFromBackup();

            RDSValidate rdsValidate = new RDSValidate(rdsClient, instanceSettings);
            rdsValidate.setDbInstance(restoredInstance);
            rdsValidate.validateResource();

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

