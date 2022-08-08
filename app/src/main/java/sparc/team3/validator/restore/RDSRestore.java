package sparc.team3.validator.restore;
/*
   Edited from: Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
   SPDX-License-Identifier: Apache-2.0
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.RecoveryPointByBackupVault;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;
import software.amazon.awssdk.services.rds.waiters.RdsWaiter;
import sparc.team3.validator.util.InstanceSettings;
import sparc.team3.validator.util.SecurityGroup;
import sparc.team3.validator.util.Util;

import java.util.Map;
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
public class RDSRestore extends AWSRestore implements Callable<DBInstance> {

    private final RdsClient rdsClient;
    private final String subnetGroupName;
    private final String[] securityGroupIDs;
    private final Logger logger;

    /**
     * Instantiates a new Rds restore.
     *
     * @param rdsClient        the rds client
     * @param instanceSettings the instanceSettings for the rds instance
     */
    public RDSRestore(BackupClient backupClient, RdsClient rdsClient, InstanceSettings instanceSettings) {
        super(backupClient, instanceSettings, "RDS");
        this.rdsClient = rdsClient;
        setUniqueNameForRestoredDBInstance();

        this.subnetGroupName = instanceSettings.getSubnetName();
        securityGroupIDs = new String[instanceSettings.getSecurityGroups().size()];
        int i = 0;
        for (SecurityGroup sg : instanceSettings.getSecurityGroups()) {
            securityGroupIDs[i] = sg.getId();
            i++;
        }

        this.logger = LoggerFactory.getLogger(this.getClass().getName());
    }

    public void setUniqueNameForRestoredDBInstance() {
    }

    @Override
    public DBInstance call() throws Exception {
        return restoreRDSFromBackup();
    }

    /**
     * Restore database instance from snapshot.
     *
     * @return the restored instance ID as a string
     */
    public DBInstance restoreRDSFromBackup() throws RecoveryPointsExhaustedException, InstanceUnavailableException {
        currentRecoveryPoint = getNextRecoveryPoint();
        // If restoring from a shared manual DB snapshot, the DBSnapshotIdentifier must be the ARN of the shared DB snapshot.
        String arn = currentRecoveryPoint.recoveryPointArn();
        logger.info("Attempting to restore rds snapshot: {}", arn);

        String uniqueNameForRestoredDBInstance = Util.UNIQUE_RESTORE_NAME_BASE + System.currentTimeMillis();

        RestoreDbInstanceFromDbSnapshotRequest request = RestoreDbInstanceFromDbSnapshotRequest
                .builder()
                .dbInstanceIdentifier(uniqueNameForRestoredDBInstance)
                .dbSnapshotIdentifier(arn)
                .dbSubnetGroupName(subnetGroupName)
                .build();

        RestoreDbInstanceFromDbSnapshotResponse response = rdsClient.restoreDBInstanceFromDBSnapshot(request);

        DBInstance restoredInstance = response.dbInstance();

        // wait for snapshot to finish restoring to a new instance
        restoredInstance = waitForInstanceToBeAvailable(restoredInstance.dbInstanceIdentifier());

        // update security group to custom one and wait
        updateSecurityGroup(restoredInstance.dbInstanceIdentifier());
        restoredInstance = waitForInstanceToBeAvailable(restoredInstance.dbInstanceIdentifier());

        // reboot for modifications to take effect immediately and wait
        rebootInstance(restoredInstance.dbInstanceIdentifier());
        restoredInstance = waitForInstanceToBeAvailable(restoredInstance.dbInstanceIdentifier());

        return restoredInstance;
    }

    /**
     * Synchronous wait to ensure database is in available state
     *
     * @param dbInstanceIdentifier the string of the database identifier
     */
    private DBInstance waitForInstanceToBeAvailable(String dbInstanceIdentifier) throws InstanceUnavailableException {
        DescribeDbInstancesRequest request = DescribeDbInstancesRequest
                .builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .build();
        RdsWaiter waiter = rdsClient.waiter();
        logger.info("Waiting for database {} to be available", dbInstanceIdentifier);
        ResponseOrException<DescribeDbInstancesResponse> responseOrException = waiter.waitUntilDBInstanceAvailable(request).matched();

        if(responseOrException.response().isPresent())
            return responseOrException.response().get().dbInstances().get(0);
        else if(responseOrException.exception().isPresent())
            throw new InstanceUnavailableException("Exception was returned instead of DB Instance", responseOrException.exception().get());
        else
            throw new InstanceUnavailableException("Exception was returned instead of DB Instance");
    }

    /**
     * Update the security group to the custom one
     *
     * @param restoredInstanceID the string of the rds instance id
     */
    private void updateSecurityGroup(String restoredInstanceID)  {
        ModifyDbInstanceRequest modifyDbRequest = ModifyDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(restoredInstanceID)
                .vpcSecurityGroupIds(securityGroupIDs)
                .applyImmediately(true)
                .build();
        logger.info("Update the security group of {} to {}", restoredInstanceID, securityGroupIDs);
        rdsClient.modifyDBInstance(modifyDbRequest);

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
    }

    /**
     * Unused for RDS
     *
     * @return Map of the metadata
     */
    @Override
    Map<String, String> setMetadata() {
        return null;
    }
}

