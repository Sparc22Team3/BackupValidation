/*
   Edited from: Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
   SPDX-License-Identifier: Apache-2.0
*/

//package com.example.rds;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceResponse;
import software.amazon.awssdk.services.rds.model.RdsException;

import java.util.*;

import static java.lang.Thread.sleep;

public class RDSRestore {

    private RdsClient rdsClient;
    private String uniqueNameForRestoredDBInstance;
    private String backupVaultName;
    private String subnetGroupName;
    private List<DBSnapshot> snapshots;

    public RDSRestore(RdsClient rdsClient, String uniqueNameForRestoredDBInstance, String backupVaultName, String subnetGroupName) {
        this.rdsClient = rdsClient;
        this.uniqueNameForRestoredDBInstance = uniqueNameForRestoredDBInstance;
        this.backupVaultName = backupVaultName;
        this.subnetGroupName = subnetGroupName;
        this.snapshots = getSnapshots();
    }

    /**
     * 1. get collection of snapshots
     * 2. restore a backup
     * 3. wait for it to be available
     * 4. update security group
     * 5. reboot instance
     * 6. wait for it to be available
     * 7. connect to EC2
     * 8. Test/validate
     * 9. log
     * 10. delete instance
     * @return
     * @throws InterruptedException
     */


    private List<DBSnapshot> getSnapshots() {
        //TreeSet<DBSnapshot> snapshotSet = new TreeSet<>();
        List<DBSnapshot> snapshotList = null;

        try {

            DescribeDbSnapshotsRequest request = DescribeDbSnapshotsRequest
                    .builder()
                    .build();

            DescribeDbSnapshotsResponse response = rdsClient.describeDBSnapshots(request);

            snapshotList = response.dbSnapshots();

        } catch (RdsException e) {
            System.out.println(e.getLocalizedMessage());
            System.exit(1);
        }
        return snapshotList;
    }

    public String restoreResource() throws InterruptedException {
        //return the hostname

        // If you are restoring from a shared manual DB snapshot, the DBSnapshotIdentifier must be the ARN of the shared DB snapshot.

        String arn = snapshots.get(0).dbSnapshotArn();

         RestoreDbInstanceFromDbSnapshotRequest request = RestoreDbInstanceFromDbSnapshotRequest
                 .builder()
                 .dbInstanceIdentifier(uniqueNameForRestoredDBInstance)
                 .dbSnapshotIdentifier(arn)
                 .dbSubnetGroupName(subnetGroupName)
                 .build();

         RestoreDbInstanceFromDbSnapshotResponse response = rdsClient.restoreDBInstanceFromDBSnapshot(request);

        String restoredInstanceID = response.dbInstance().dbInstanceIdentifier();
        System.out.println("restoredInstanceID : " +restoredInstanceID);
        System.out.println("response class :: " +response.getClass());
        System.out.println("sleeping for 5 minutes now");
        sleep(300000);
        //rdsClient.waiter().waitUntilDBInstanceAvailable();

        // update security group to custom one
        updateSecurityGroup(response, "sg-078715763233fad97");

        System.out.println("sleeping for 5 minutes now");
        sleep(300000);

        //delete db instance after testing
        deleteDBTestInstance(restoredInstanceID);

        return response.dbInstance().dbInstanceIdentifier();
    }


    public void updateSecurityGroup(RestoreDbInstanceFromDbSnapshotResponse response, String securityGroupID) {
    //public void updateSecurityGroup() {
        //if (response.dbInstance().dbInstanceStatus() != "available")
        ModifyDbInstanceRequest modifyDbRequest = ModifyDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(response.dbInstance().dbInstanceIdentifier())
                .vpcSecurityGroupIds(securityGroupID)
                .build();

        ModifyDbInstanceResponse modifyDBResponse = rdsClient.modifyDBInstance(modifyDbRequest);
        System.out.println(" Modified db restore, vpc security group: " + modifyDBResponse.dbInstance().vpcSecurityGroups());
        System.out.println("dbInstanceStatus " +modifyDBResponse.dbInstance().dbInstanceStatus());
       // if (modifyDBResponse.dbInstance().dbInstanceStatus() != "available")
        //modifyDbRequest.vpcSecurityGroupIds().add("sg-078715763233fad97");
        System.out.println("value of modify db response : " +String.valueOf(modifyDBResponse));

    }

    //public void deleteDBTestInstance(String dbInstanceIdentifier) {
    public void deleteDBTestInstance(String restoredInstanceID) {

        DeleteDbInstanceRequest deleteRequest = DeleteDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(restoredInstanceID)
                .skipFinalSnapshot(true)
                .build();
        DeleteDbInstanceResponse deleteResponse = rdsClient.deleteDBInstance(deleteRequest);
    }

    /** modified from:
     * https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/rds/src/main/java/com/example/rds/DescribeDBInstances.java */
   // public void describeSnapshots(RdsClient rdsClient) {
    public void describeSnapshots() {

        try {

            // describe the RDS snapshots
            DescribeDbSnapshotsRequest request = DescribeDbSnapshotsRequest
                    .builder()
                    // to get a specific snapshot:
                    //.dbSnapshotIdentifier("arn:aws:rds:us-east-1:490610433117:snapshot:awsbackup:job-80f5fbc6-2e1b-ed56-3cf0-71df898be9c6")
                    .build();

            DescribeDbSnapshotsResponse responseS = rdsClient.describeDBSnapshots(request);

            // List is auto-sorted by ARN / RecoveryPointID; ie. not the most recent backup
            List<DBSnapshot> snapshotList = responseS.dbSnapshots();

 /**           System.out.println("size of snapshot list: " + snapshotList.size());
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
  */

        } catch (RdsException e) {
            System.out.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }

}

//restore job NOTES

/**       WindowStateListener listener = new WindowStateListener() {
@Override
public void windowStateChanged(WindowEvent e) {

}
}
 */


/**
 RestoreDBInstanceFromDBSnapshotRequest restoreDBInstanceRequest = new RestoreDBInstanceFromDBSnapshotRequest();
 restoreDBInstanceRequest.setDBSnapshotIdentifier("restoredDB");
 restoreDBInstanceRequest.setDBName(rdsDev);
 restoreDBInstanceRequest.setDBSubnetGroupName(subnetGroup);
 restoreDBInstanceRequest.setAvailabilityZone(availabilityZone);
 this.rds.restoreDBInstanceFromDBSnapshot(restoreDBInstanceRequest);
 */

