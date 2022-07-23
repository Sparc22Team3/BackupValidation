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

import java.util.List;

import static java.lang.Thread.sleep;

public class RDSRestore {

    RdsClient rdsClient;
    String backupVaultName;
    List<DBSnapshot> snapshotList;
   // TreeSet

    public RDSRestore(RdsClient rdsClient, String backupVaultName) {
        this.rdsClient = rdsClient;
        this.backupVaultName = backupVaultName;
        this.snapshotList = getListOfSnapshots();
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
    public String restoreResource() throws InterruptedException {
        //return the hostname

        // If you are restoring from a shared manual DB snapshot, the DBSnapshotIdentifier must be the ARN of the shared DB snapshot.

        //String dbSnapShotIdentifier = String.valueOf(snapshotList.get(0));
        //System.out.println(dbSnapShotIdentifier);
        System.out.println();

         RestoreDbInstanceFromDbSnapshotRequest request = RestoreDbInstanceFromDbSnapshotRequest
                 .builder()
                 .dbInstanceIdentifier("database-TEST" +System.currentTimeMillis()) //put in util
                 //.dbSnapshotIdentifier(dbSnapShotIdentifier)
                 .dbSnapshotIdentifier("arn:aws:rds:us-east-1:490610433117:snapshot:awsbackup:job-c3fbf429-a4bb-0237-d0bd-9775134de8df")
                 //.vpcSecurityGroupIds("vpc-02926b86fed57e4e5")
                 .dbSubnetGroupName("team3-sparc-db-subnet-group") //add to Settings (security class?
                 .build();

         RestoreDbInstanceFromDbSnapshotResponse response = rdsClient.restoreDBInstanceFromDBSnapshot(request);

        CreateDbInstanceRequest instanceRequest = CreateDbInstanceRequest
                .builder()
                .masterUserPassword("WHAt is it")
                .build();

        CreateDbInstanceResponse instanceResponse = rdsClient.createDBInstance(instanceRequest);
        System.out.print("The status is " + instanceResponse.dbInstance().dbInstanceStatus());
        System.out.print("The identifier is " + instanceResponse.dbInstance().dbInstanceIdentifier());
        System.out.print("The vpc security group is " + instanceResponse.dbInstance().vpcSecurityGroups());
        System.out.print("The db security is " + instanceResponse.dbInstance().dbSecurityGroups());

         // vpc security group IDs is empty; if there is one, might be able to update there in the restore rather than afterwards
         //System.out.println("vpcSecurityGroupIds : " +request.vpcSecurityGroupIds());
         //System.out.println("dbSubnetGroupName : " +request.dbSubnetGroupName());

        String restoredInstanceID = response.dbInstance().dbInstanceIdentifier();
        System.out.println("restoredInstanceID : " +restoredInstanceID);
        System.out.println("sleeping for 5 minutes now");
        sleep(300000);
        //rdsClient.waiter().waitUntilDBInstanceAvailable();
        updateSecurityGroup(response);

        // the modified security group is reflected in console, but not with print statements below
        String identifier = response.dbInstance().dbInstanceIdentifier();
        System.out.println("identifier: " + identifier);
        System.out.println("vpc security group: " + response.dbInstance().vpcSecurityGroups());

        System.out.println("sleeping for 3 minutes now");
     //   sleep(180000);
     //   deleteDBTestInstance(response);

        //return response1.dbInstance();
        //return response1.responseMetadata();
        return response.toString();
    }

    List<DBSnapshot>  getListOfSnapshots() {

        //TreeMap<Instance, DBSnapshot> mapSortedByCreateTimeRDSSnapshots = new TreeMap<>();

        try {

            // describe the RDS snapshots
            DescribeDbSnapshotsRequest request = DescribeDbSnapshotsRequest
                    .builder()
                    .build();

            DescribeDbSnapshotsResponse responseS = rdsClient.describeDBSnapshots(request);
            System.out.println("RESPONSE snapshots: " +responseS.dbSnapshots());

            // List is sorted by dbSnapshotIdentifier
            // not allowed to sort (unmodifiable list) // snapshotList.sort(null);
            // so put in TreeMap sorted by SnapshotCreateTime
            List<DBSnapshot> snapshotList = responseS.dbSnapshots();
            // 0th element is not the most recent; it's a random one, but order is preserved across runs
            System.out.println("id :: " +snapshotList.get(0).dbSnapshotIdentifier());
            System.out.println("arn :: " +snapshotList.get(0).dbSnapshotArn());

        } catch (RdsException e) {
            System.out.println(e.getLocalizedMessage());
            System.exit(1);
        }
        return snapshotList;
    }

    public void updateSecurityGroup(RestoreDbInstanceFromDbSnapshotResponse response) {
    //public void updateSecurityGroup() {
        //if (response.dbInstance().dbInstanceStatus() != "available")
        ModifyDbInstanceRequest modifyDbRequest = ModifyDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(response.dbInstance().dbInstanceIdentifier())
                //.vpcSecurityGroupIds("sg-078715763233fad97")
                .build();

        ModifyDbInstanceResponse modifyDBResponse = rdsClient.modifyDBInstance(modifyDbRequest);
        System.out.println(" Modified db restore, vpc security group: " + modifyDBResponse.dbInstance().vpcSecurityGroups());
        System.out.println("dbInstanceStatus " +modifyDBResponse.dbInstance().dbInstanceStatus());
       // if (modifyDBResponse.dbInstance().dbInstanceStatus() != "available")
        modifyDbRequest.vpcSecurityGroupIds().add("sg-078715763233fad97");
        System.out.println("value of modify db response : " +String.valueOf(modifyDBResponse));

    }

    //public void deleteDBTestInstance(String dbInstanceIdentifier) {
    public void deleteDBTestInstance(RestoreDbInstanceFromDbSnapshotResponse response) {

        DeleteDbInstanceRequest deleteRequest = DeleteDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(response.dbInstance().dbInstanceIdentifier())
                .build();
        deleteRequest.skipFinalSnapshot();
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
            System.out.println("RESPONSE snapshots: " +responseS.dbSnapshots());

            // List is sorted by dbSnapshotIdentifier
            // not allowed to sort (unmodifiable list) // snapshotList.sort(null);
             // so put in TreeMap sorted by SnapshotCreateTime
            List<DBSnapshot> snapshotList = responseS.dbSnapshots();
            // 0th element is not the most recent; it's a random one
            System.out.println("id :: " +snapshotList.get(1).dbSnapshotIdentifier());
            System.out.println("arn :: " +snapshotList.get(1).dbSnapshotArn());

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

