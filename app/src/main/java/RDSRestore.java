/*
   Edited from: Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
   SPDX-License-Identifier: Apache-2.0
*/

//package com.example.rds;

// snippet-start:[rds.java2.describe_instances.import]
import software.amazon.awssdk.services.backup.model.RecoveryPointByBackupVault;
import software.amazon.awssdk.services.backup.model.StartRestoreJobRequest;
import software.amazon.awssdk.services.backup.model.StartRestoreJobResponse;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceResponse;
import software.amazon.awssdk.services.rds.model.RdsException;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Thread.sleep;
import static jdk.nashorn.internal.objects.NativeDate.getTime;

public class RDSRestore {

    RdsClient rdsClient;
    String backupVaultName;

    public RDSRestore(RdsClient rdsClient, String backupVaultName) {
        this.rdsClient = rdsClient;
        this.backupVaultName = backupVaultName;
    }

    /**
     * 1. restore a backup
     * 2. wait for it to be available
     * 3. update security group
     * 4. reboot instance
     * 5. wait for it to be available
     * 6. connect to EC2
     * 7. Test/validate
     * 8. log
     * 9. delete instance
     * @return
     * @throws InterruptedException
     */
    public String restoreResource() throws InterruptedException {

        // If you are restoring from a shared manual DB snapshot, the DBSnapshotIdentifier must be the ARN of the shared DB snapshot.

        Long startTime = System.nanoTime();
         RestoreDbInstanceFromDbSnapshotRequest request = RestoreDbInstanceFromDbSnapshotRequest
                 .builder()
                 .dbInstanceIdentifier("database-TEST-6")
                 .dbSnapshotIdentifier("arn:aws:rds:us-east-1:490610433117:snapshot:awsbackup:job-80f5fbc6-2e1b-ed56-3cf0-71df898be9c6")
                 //.vpcSecurityGroupIds("vpc-02926b86fed57e4e5")
                 //.vpcSecurityGroupIds()
                 .dbSubnetGroupName("team3-sparc-db-subnet-group")
                 .build();

         RestoreDbInstanceFromDbSnapshotResponse response = rdsClient.restoreDBInstanceFromDBSnapshot(request);

         System.out.println("vpcSecurityGroupIds : " +request.vpcSecurityGroupIds());
         System.out.println("dbSubnetGroupName : " +request.dbSubnetGroupName());

         System.out.println(" RESPONSE :: " + response.toString());
         System.out.println(" INSTANCE :: " + response.dbInstance());
         System.out.println(" RESPONSE META-DATA REQUEST ID :: " + response.responseMetadata().requestId());
         // System.out.println(" RESPONSE META-DATA :: " + response.responseMetadata().toString());

        long elapsedTime = System.nanoTime() - startTime;
        System.out.println(elapsedTime);
        sleep(300000);

 /**       WindowStateListener listener = new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {

            }
        }
  */

        modifyAttributes(response);

        String identifier = response.dbInstance().dbInstanceIdentifier();
        System.out.println("identifier: " + identifier);
        System.out.println(" RESPONSE vpc security group: " + response.dbInstance().vpcSecurityGroups());

        //return response1.dbInstance();
        //return response1.responseMetadata();
        return response.toString();
    }

    public void modifyAttributes(RestoreDbInstanceFromDbSnapshotResponse response) {
        ModifyDbInstanceRequest modifyDbRequest = ModifyDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(response.dbInstance().dbInstanceIdentifier())
                .vpcSecurityGroupIds("sg-078715763233fad97")
                .build();
        ModifyDbInstanceResponse modifyDBResponse = rdsClient.modifyDBInstance(modifyDbRequest);

    }

    /** modified from:
     * https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/rds/src/main/java/com/example/rds/DescribeDBInstances.java */
    public void describeSnapshots(RdsClient rdsClient) {

        try {

            // describe the RDS snapshots
            DescribeDbSnapshotsRequest request = DescribeDbSnapshotsRequest
                    .builder()
                    // to get a specific snapshot:
                    //.dbSnapshotIdentifier("arn:aws:rds:us-east-1:490610433117:snapshot:awsbackup:job-80f5fbc6-2e1b-ed56-3cf0-71df898be9c6")
                    .build();

            DescribeDbSnapshotsResponse responseS = rdsClient.describeDBSnapshots(request);
            System.out.println("RESPONSE snapshots: " +responseS.dbSnapshots());

            /** List is sorted by dbSnapshotIdentifier
             * not allowed to sort (unmodifiable list) // snapshotList.sort(null);
             * so put in TreeMap sorted by SnapshotCreateTime */
            List<DBSnapshot> snapshotList = responseS.dbSnapshots();

            System.out.println("size of snapshot list: " + snapshotList.size());
            for (DBSnapshot snapshot : snapshotList) {
                System.out.println("Snapshot ARN is: "+snapshot.dbSnapshotArn());
                System.out.println("Snapshot identifier is: " +snapshot.dbSnapshotIdentifier());
                // System.out.println("The Engine is " +snapshot.engine());
                // System.out.println("Snapshot create time is " +snapshot.snapshotCreateTime());
                System.out.println("DB instance identifier is " +snapshot.dbInstanceIdentifier());
                System.out.println("VPC ID is: "+snapshot.vpcId());
                //snapshot.getValueForField("DBSecurityGroup", rdsClient);
            }


            // describe the RDS instances
            DescribeDbInstancesRequest requestI = DescribeDbInstancesRequest
                    .builder()
                    .dbInstanceIdentifier("arn:aws:rds:us-east-1:490610433117:db:database-test")
                    .build();
            DescribeDbInstancesResponse responseI = rdsClient.describeDBInstances();
            List<DBInstance> instanceList = responseI.dbInstances();
            System.out.printf("%n");
            System.out.println("size of instance list: " + instanceList.size());
            for (DBInstance instance: instanceList) {
                System.out.println("Instance ARN is: "+instance.dbInstanceArn());
                // System.out.println("The Engine is " +instance.engine());
                System.out.println("Connection endpoint is " +instance.endpoint().address());
                System.out.println("dbInstanceIdentifier " + instance.dbInstanceIdentifier());
                System.out.println("vpcSecurityGroups " + instance.vpcSecurityGroups());
                System.out.println("dbSecurityGroups " + instance.dbSecurityGroups());
                System.out.println("dbSubnetGroup " + instance.dbSubnetGroup());

            }

        } catch (RdsException e) {
            System.out.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }

    /** modified from:
     * https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/rds/src/main/java/com/example/rds/ModifyDBInstance.java */
    public  void updateDbName(RdsClient rdsClient, String dbInstanceIdentifier) {

        try {
            ModifyDbInstanceRequest request = ModifyDbInstanceRequest
                    .builder()
                    .dbInstanceIdentifier(dbInstanceIdentifier)
                    .build();

            ModifyDbInstanceResponse response = rdsClient.modifyDBInstance(request);
            System.out.print("The ARN of the modified database is: " +response.dbInstance().dbInstanceArn());

        } catch (RdsException e) {
            System.out.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }

    /**
    public RecoveryPointByBackupVault getRecentRecoveryPoint(int recoveryNumber) throws Exception{

        if (recoveryNumber > recoveryPoints.size()){

            throw new Exception("Recovery Points Exhausted");

        }

        return recoveryPoints.get(recoveryPoints.keySet().toArray()[recoveryNumber]);
    }
     */
}

//restore job
// ?? // client.startRestoreJob(StartRestoreJobRequest.builder().build());
// RestoreDBInstanceFromDBSnapshot(nameOfDB); can that parameter be a new name b/c not replacing?

// https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/rds/model/RestoreDBInstanceFromDBSnapshotRequest.html
// RestoreDBInstanceFromDBSnapshotRequest(String restored_DB, client.getDBInstanceIdentifier());

//StartRestoreJobRequest.Builder r = StartRestoreJobRequest.builder().recoveryPointArn(String.valueOf(recoveryPoint));
//StartRestoreJobRequest r = StartRestoreJobRequest.builder().recoveryPointArn(recoveryPoint.recoveryPointArn()).iamRoleArn(recoveryPoint.iamRoleArn())
//      .metadata(metadata).build();

// default StartRestoreJobResponse startRestoreJob(StartRestoreJobRequest startRestoreJobRequest)
/**
 RestoreDBInstanceFromDBSnapshotRequest restoreDBInstanceRequest = new RestoreDBInstanceFromDBSnapshotRequest();
 restoreDBInstanceRequest.setDBSnapshotIdentifier("restoredDB");
 restoreDBInstanceRequest.setDBName(rdsDev);
 restoreDBInstanceRequest.setDBSubnetGroupName(subnetGroup);
 restoreDBInstanceRequest.setAvailabilityZone(availabilityZone);
 this.rds.restoreDBInstanceFromDBSnapshot(restoreDBInstanceRequest);


 String arn = String.valueOf(recoveryPoint);
 RestoreDBInstanceFromDBSnapshotRequest request = new RestoreDbInstanceFromDbSnapshotRequest();
 RestoreDbInstanceFromDbSnapshotRequest.Builder restoredDB = RestoreDbInstanceFromDbSnapshotRequest.builder().dbInstanceIdentifier("restoredDB");

 StartRestoreJobResponse response = client.startRestoreJob((Consumer<StartRestoreJobRequest.Builder>) restoredDB);
 return response.restoreJobId();
 */

