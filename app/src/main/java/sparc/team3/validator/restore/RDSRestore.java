package sparc.team3.validator.restore;
/*
   Edited from: Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
   SPDX-License-Identifier: Apache-2.0
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;
import software.amazon.awssdk.services.rds.waiters.RdsWaiter;
import sparc.team3.validator.util.InstanceSettings;
import sparc.team3.validator.util.SecurityGroup;
import sparc.team3.validator.util.Util;

import java.util.List;
import java.util.concurrent.Callable;


/**
 * This class restores an RDS instance from a snapshot
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
 */
public class RDSRestore implements Callable<DBInstance> {

    private final RdsClient rdsClient;
    private final String backupVaultName;
    private final String subnetGroupName;
    private final String[] securityGroupIDs;
    private final List<DBSnapshot> snapshots;
    private final Logger logger;
    private String uniqueNameForRestoredDBInstance;

    /**
     * Instantiates a new Rds restore.
     *
     * @param rdsClient        the rds client
     * @param instanceSettings the instanceSettings for the rds instance
     */
    public RDSRestore(RdsClient rdsClient, InstanceSettings instanceSettings) {

        this.rdsClient = rdsClient;
        setUniqueNameForRestoredDBInstance();
        this.backupVaultName = instanceSettings.getBackupVault();
        this.subnetGroupName = instanceSettings.getSubnetName();
        securityGroupIDs = new String[instanceSettings.getSecurityGroups().size()];
        int i = 0;
        for (SecurityGroup sg : instanceSettings.getSecurityGroups()) {
            securityGroupIDs[i] = sg.getId();
            i++;
        }
        this.snapshots = getSnapshots();
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
    }

    public void setUniqueNameForRestoredDBInstance() {
        uniqueNameForRestoredDBInstance = Util.UNIQUE_RESTORE_NAME_BASE + System.currentTimeMillis();
    }

    /**
     * Gets the list of current RDS snapshots
     *
     * @return List of DBSnapshots
     */
    private List<DBSnapshot> getSnapshots() {
        List<DBSnapshot> snapshotList;

        try {
            DescribeDbSnapshotsRequest request = DescribeDbSnapshotsRequest
                    .builder()
                    .build();

            DescribeDbSnapshotsResponse response = rdsClient.describeDBSnapshots(request);

            snapshotList = response.dbSnapshots();

        } catch (RdsException e) {
            logger.error("Problem getting list of database snapshots", e);
            return null;
        }
        return snapshotList;
    }

    @Override
    public DBInstance call() {
        return restoreRDSFromBackup();
    }

    /**
     * Restore database instance from snapshot.
     *
     * @return the restored instance ID as a string
     */
    public DBInstance restoreRDSFromBackup() {
        if (snapshots == null)
            return null;
        // If restoring from a shared manual DB snapshot, the DBSnapshotIdentifier must be the ARN of the shared DB snapshot.
        String arn = snapshots.get(0).dbSnapshotArn();
        logger.info("Attempting to restore rds snapshot: {}", arn);

        RestoreDbInstanceFromDbSnapshotRequest request = RestoreDbInstanceFromDbSnapshotRequest
                .builder()
                .dbInstanceIdentifier(uniqueNameForRestoredDBInstance)
                .dbSnapshotIdentifier(arn)
                .dbSubnetGroupName(subnetGroupName)
                .build();

        RestoreDbInstanceFromDbSnapshotResponse response = rdsClient.restoreDBInstanceFromDBSnapshot(request);

        DBInstance restoredInstance = response.dbInstance();

        // wait for snapshot to finish restoring to a new instance
        waitForInstanceToBeAvailable(restoredInstance.dbInstanceIdentifier());

        // update security group to custom one
        updateSecurityGroup(restoredInstance.dbInstanceIdentifier());

        // reboot for modifications to take effect immediately
        rebootInstance(restoredInstance.dbInstanceIdentifier());

        return restoredInstance;
    }

    /**
     * Synchronous wait to ensure database is in available state
     *
     * @param dbInstanceIdentifier the string of the database identifier
     */
    private void waitForInstanceToBeAvailable(String dbInstanceIdentifier) {
        DescribeDbInstancesRequest request = DescribeDbInstancesRequest
                .builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .build();
        RdsWaiter waiter = rdsClient.waiter();
        logger.info("Waiting for database {} to be available", dbInstanceIdentifier);
        waiter.waitUntilDBInstanceAvailable(request);
    }

    /**
     * Update the security group to the custom one
     *
     * @param restoredInstanceID the string of the rds instance id
     */
    private void updateSecurityGroup(String restoredInstanceID) {
        ModifyDbInstanceRequest modifyDbRequest = ModifyDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(restoredInstanceID)
                .vpcSecurityGroupIds(securityGroupIDs)
                .applyImmediately(true)
                .build();
        logger.info("Update the security group of {} to {}", restoredInstanceID, securityGroupIDs);
        rdsClient.modifyDBInstance(modifyDbRequest);
        waitForInstanceToBeAvailable(restoredInstanceID);
    }

    /**
     * Reboot the database instance to ensure any change requests are complete
     *
     * @param dbInstanceIdentifier the string of the rds instance id
     */
    private void rebootInstance(String dbInstanceIdentifier) {
        RebootDbInstanceRequest request = RebootDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .build();
        logger.info("Rebooting database {}", dbInstanceIdentifier);
        rdsClient.rebootDBInstance(request);
        waitForInstanceToBeAvailable(dbInstanceIdentifier);
    }

}

/**
 * WindowStateListener listener = new WindowStateListener() {
 *
 * @Override public void windowStateChanged(WindowEvent e) {
 * <p>
 * }
 * }
 */

