package sparc.team3.validator.restore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.*;
import sparc.team3.validator.util.InstanceSettings;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public abstract class AWSRestore {
    final BackupClient backupClient;
    final InstanceSettings instanceSettings;
    final TreeMap<Instant, RecoveryPointByBackupVault> recoveryPoints;
    RecoveryPointByBackupVault currentRecoveryPoint;
    Map<String, String> metadata;
    final Logger logger;
    int recoveryNumber;


    public AWSRestore(BackupClient backupClient, InstanceSettings instanceSettings) {
        this.backupClient = backupClient;
        this.instanceSettings = instanceSettings;
        this.recoveryPoints = getRecoveryPoints(instanceSettings.getBackupVault());
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
        recoveryNumber = 0;
    }

    /**
     * Start the restore job for a given recovery point.
     * @return AWSRestore Job ID
     */
    synchronized String startRestore() {
        try {
            currentRecoveryPoint = getRecentRecoveryPoint();
        } catch (Exception e) {
            return null;
        }
        metadata = setMetadata();
        logger.info("Attempting to restore recovery point: {}", currentRecoveryPoint.recoveryPointArn());

        StartRestoreJobRequest request = StartRestoreJobRequest.builder().
                recoveryPointArn(currentRecoveryPoint.recoveryPointArn()).iamRoleArn(currentRecoveryPoint.iamRoleArn())
                .metadata(metadata).build();

        StartRestoreJobResponse response = backupClient.startRestoreJob(request);

        return response.restoreJobId();

    }

    /**
     * Set the metadata required for restore
     * @return Map of the metadata
     */
    abstract Map<String, String> setMetadata();

    /**
     * Method gets a list of backup restore points from backup vault and populates a sorted data structure.
     * @param backupVaultName the string name of the backup vault to retrieve recovery points from
     * @return a TreeMap of the recovery points in the backup vault
     *
     */
    TreeMap<Instant, RecoveryPointByBackupVault> getRecoveryPoints(String backupVaultName){

        TreeMap<Instant, RecoveryPointByBackupVault> output =
                new TreeMap<>(Collections.reverseOrder());

        //call and response with amazon to get list of vault backups
        ListRecoveryPointsByBackupVaultRequest request =
                ListRecoveryPointsByBackupVaultRequest.builder().
                        backupVaultName(backupVaultName).build();

        ListRecoveryPointsByBackupVaultResponse response =
                backupClient.listRecoveryPointsByBackupVault(request);

        for(RecoveryPointByBackupVault r:
                response.recoveryPoints()){

            output.put(r.creationDate(), r);
        }

        return output;
    }

    /**
     * Return most recent recovery point from vault.
     * @return a RecoveryPointByBackupVault
     * @throws Exception when recovery points have been exhausted
     */

    synchronized RecoveryPointByBackupVault getRecentRecoveryPoint() throws Exception{

        if (recoveryNumber > recoveryPoints.size()){

            throw new Exception("Recovery Points Exhausted");

        }

        return recoveryPoints.get(recoveryPoints.keySet().toArray()[recoveryNumber]);
    }

    public synchronized void incrementRecoveryNumber(){
        recoveryNumber++;
    }

    public synchronized int getRecoveryNumber(){
        return recoveryNumber;
    }
}
