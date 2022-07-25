/*
   Edited from: Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
   SPDX-License-Identifier: Apache-2.0
*/

//package com.example.rds;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.RdsException;
import software.amazon.awssdk.services.rds.waiters.RdsWaiter;
import software.amazon.awssdk.utils.HostnameValidator;

import javax.net.ssl.HostnameVerifier;
import java.util.*;


/**
 * This class restores an RDS instance from a snapshot
 */
public class RDSRestore {

    private RdsClient rdsClient;
    private String uniqueNameForRestoredDBInstance;
    private String backupVaultName;
    private String subnetGroupName;
    private String securityGroupID;
    private List<DBSnapshot> snapshots;

    /**
     * Instantiates a new Rds restore.
     *
     * @param rdsClient                       the rds client
     * @param uniqueNameForRestoredDBInstance the unique name for restored db instance
     * @param backupVaultName                 the backup vault name
     * @param subnetGroupName                 the subnet group name
     * @param securityGroupID                 the security group id
     */
    public RDSRestore(RdsClient rdsClient, String uniqueNameForRestoredDBInstance,
                      String backupVaultName, String subnetGroupName,String securityGroupID) {
        this.rdsClient = rdsClient;
        this.uniqueNameForRestoredDBInstance = uniqueNameForRestoredDBInstance;
        this.backupVaultName = backupVaultName;
        this.subnetGroupName = subnetGroupName;
        this.securityGroupID = securityGroupID;
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


    /**
     * Gets the list of current RDS snapshots
     * @return List of DBSnapshots
     */
    private List<DBSnapshot> getSnapshots() {
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

    /**
     * Restore database instance from snapshot.
     *
     * @return the restored instance ID as a string
     * @throws InterruptedException the interrupted exception
     */
    public String restoreResource() throws InterruptedException {
        // If restoring from a shared manual DB snapshot, the DBSnapshotIdentifier must be the ARN of the shared DB snapshot.

        String arn = snapshots.get(0).dbSnapshotArn();

         RestoreDbInstanceFromDbSnapshotRequest request = RestoreDbInstanceFromDbSnapshotRequest
                 .builder()
                 .dbInstanceIdentifier(uniqueNameForRestoredDBInstance)
                 .dbSnapshotIdentifier(arn)
                 .dbSubnetGroupName(subnetGroupName)
                 .build();

         RestoreDbInstanceFromDbSnapshotResponse response = rdsClient.restoreDBInstanceFromDBSnapshot(request);

         String restoredInstanceID = response.dbInstance().dbInstanceIdentifier();

         // wait for snapshot to finish restoring to a new instance
         waitForInstanceToBeAvailable(restoredInstanceID);

         // update security group to custom one
         updateSecurityGroup(restoredInstanceID, securityGroupID);

         // reboot for modifications to take effect immediately
         rebootInstance(restoredInstanceID);

         return restoredInstanceID;
    }


    /**
     * Synchronous wait to ensure database is in available state
     * @param dbInstanceIdentifier
     */
    private void waitForInstanceToBeAvailable(String dbInstanceIdentifier) {
        DescribeDbInstancesRequest request = DescribeDbInstancesRequest
                .builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .build();
        RdsWaiter waiter = rdsClient.waiter();

        waiter.waitUntilDBInstanceAvailable(request);
    }


    /**
     * Update the security group to the custom one
     * @param restoredInstanceID
     * @param securityGroupID
     */
    private void updateSecurityGroup(String restoredInstanceID, String securityGroupID) {
        ModifyDbInstanceRequest modifyDbRequest = ModifyDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(restoredInstanceID)
                .vpcSecurityGroupIds(securityGroupID)
                .applyImmediately(true)
                .build();

        rdsClient.modifyDBInstance(modifyDbRequest);
        waitForInstanceToBeAvailable(restoredInstanceID);
    }

    /**
     * Reboot the database instance to ensure any change requests are complete
     * @param dbInstanceIdentifier
     */
    private void rebootInstance(String dbInstanceIdentifier) {
        RebootDbInstanceRequest request = RebootDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .build();

        rdsClient.rebootDBInstance(request);
        waitForInstanceToBeAvailable(dbInstanceIdentifier);
    }

}

/**       WindowStateListener listener = new WindowStateListener() {
@Override
public void windowStateChanged(WindowEvent e) {

}
}
 */

