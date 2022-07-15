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

import java.util.List;
import java.util.function.Consumer;

public class RDSRestore {

    RdsClient rdsClient;
    String backupVaultName;

    public RDSRestore(RdsClient rdsClient, String backupVaultName) {
        this.rdsClient = rdsClient;
        this.backupVaultName = backupVaultName;
    }

    // public String restoreResource(String arn) {
    public String restoreResource() {

        // If you are restoring from a shared manual DB snapshot, the DBSnapshotIdentifier must be the ARN of the shared DB snapshot.

/**        StartRestoreJobRequest r = StartRestoreJobRequest
                .builder()
                .recoveryPointArn(arn)
                //.recoveryPointArn("arn:aws:rds:us-east-1:490610433117:snapshot:awsbackup:job-43482459-397a-8939-a116-353685d5d075")
                .iamRoleArn("arn:aws:iam::490610433117:role/service-role/AWSBackupDefaultServiceRole")
                .build();
        StartRestoreJobResponse s = rdsClient.startDBInstanceAutomatedBackupsReplication(r)
                .builder()
                .build();
 */

         RestoreDbInstanceFromDbSnapshotRequest request = RestoreDbInstanceFromDbSnapshotRequest
                 .builder()
                 .dbInstanceIdentifier("database-test-2-DELETE")
                 .dbSnapshotIdentifier("arn:aws:rds:us-east-1:490610433117:snapshot:awsbackup:job-527630e8-9d40-091d-7f0e-1037b90c4025")
                 .build();

         RestoreDbInstanceFromDbSnapshotResponse response = rdsClient.restoreDBInstanceFromDBSnapshot(request);

         System.out.println(" RESPONSE :: " + response.toString());
         System.out.println(" INSTANCE :: " + response.dbInstance());
         System.out.println(" RESPONSE META-DATA REQUEST ID :: " + response.responseMetadata().requestId());
         // System.out.println(" RESPONSE META-DATA :: " + response.responseMetadata().toString());

        //return response1.dbInstance();
        //return response1.responseMetadata();
        return response.toString();
    }

    /** modified from:
     * https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/rds/src/main/java/com/example/rds/DescribeDBInstances.java */
    public void describeSnapshots(RdsClient rdsClient) {

        try {
            DescribeDbSnapshotsResponse requestS = rdsClient.describeDBSnapshots();
            List<DBSnapshot> snapshotList = requestS.dbSnapshots();
            System.out.println("size of snapshot list: " + snapshotList.size());
            for (DBSnapshot snapshot : snapshotList) {
                System.out.println("Snapshot ARN is: "+snapshot.dbSnapshotArn());
                System.out.println("Snapshot identifier is: " +snapshot.dbSnapshotIdentifier());
                System.out.println("The Engine is " +snapshot.engine());
                System.out.println("Snapshot create time is " +snapshot.snapshotCreateTime());
                System.out.println("DB instance identifier is " +snapshot.dbInstanceIdentifier());
            }

            // describe the RDS instance
            DescribeDbInstancesResponse responseI = rdsClient.describeDBInstances();
            List<DBInstance> instanceList = responseI.dbInstances();
            for (DBInstance instance: instanceList) {
                System.out.println("Instance ARN is: "+instance.dbInstanceArn());
                System.out.println("The Engine is " +instance.engine());
                System.out.println("Connection endpoint is " +instance.endpoint().address());
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

