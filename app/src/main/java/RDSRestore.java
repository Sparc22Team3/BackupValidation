/*
   Edited from: Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
   SPDX-License-Identifier: Apache-2.0
*/

//package com.example.rds;

import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.ModifyDbInstanceResponse;
import software.amazon.awssdk.services.rds.model.RdsException;
import software.amazon.awssdk.services.rds.waiters.RdsWaiter;

import java.util.*;

import static java.lang.Thread.sleep;

public class RDSRestore {

    private RdsClient rdsClient;
    private String uniqueNameForRestoredDBInstance;
    private String backupVaultName;
    private String subnetGroupName;
    private String securityGroupID;
    private List<DBSnapshot> snapshots;

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

    public String restoreResource() throws InterruptedException {
        //return the hostname

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

         waitForInstanceToBeAvailable(restoredInstanceID);

         // update security group to custom one
         updateSecurityGroup(restoredInstanceID, securityGroupID);

         waitForInstanceToBeAvailable(restoredInstanceID);

         rebootInstance(restoredInstanceID);

         waitForInstanceToBeAvailable(restoredInstanceID);

         //delete db instance after testing
         deleteDBTestInstance(restoredInstanceID);

         return response.dbInstance().dbInstanceIdentifier();
    }

    // synchronous wait with rds waiter
    private void waitForInstanceToBeAvailable(String dbInstanceIdentifier) {
        DescribeDbInstancesRequest request = DescribeDbInstancesRequest
                .builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .build();
        RdsWaiter waiter = rdsClient.waiter();

        waiter.waitUntilDBInstanceAvailable(request);
    }

    private void updateSecurityGroup(String restoredInstanceID, String securityGroupID) {
        ModifyDbInstanceRequest modifyDbRequest = ModifyDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(restoredInstanceID)
                .vpcSecurityGroupIds(securityGroupID)
                .build();

        rdsClient.modifyDBInstance(modifyDbRequest);
    }

    private void rebootInstance(String dbInstanceIdentifier) {
        RebootDbInstanceRequest request = RebootDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .build();

        rdsClient.rebootDBInstance(request);
    }

    private void deleteDBTestInstance(String dbInstanceIdentifier) {

        DeleteDbInstanceRequest deleteRequest = DeleteDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .skipFinalSnapshot(true)
                .build();

        rdsClient.deleteDBInstance(deleteRequest);
    }

}

//restore job NOTES

/**       WindowStateListener listener = new WindowStateListener() {
@Override
public void windowStateChanged(WindowEvent e) {

}
}
 */

