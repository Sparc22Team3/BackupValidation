package sparc.team3.validator.restore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.*;
import sparc.team3.validator.util.InstanceSettings;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

public abstract class AWSRestore {
    final BackupClient backupClient;
    final InstanceSettings instanceSettings;
    final TreeSet<RecoveryPointByBackupVault> recoveryPoints;
    RecoveryPointByBackupVault currentRecoveryPoint;
    Map<String, String> metadata;
    final Logger logger;
    final String resourceType;
    Iterator<RecoveryPointByBackupVault> recoveryPointSetIterator;


    public AWSRestore(BackupClient backupClient, InstanceSettings instanceSettings, String resourceType) {
        this.backupClient = backupClient;
        this.instanceSettings = instanceSettings;
        this.resourceType = resourceType;
        this.recoveryPoints = getRecoveryPoints(instanceSettings.getBackupVault());
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
    }

    /**
     * Start the restore job for a given recovery point.
     * @return AWSRestore Job ID
     */
    synchronized String startRestore() throws RecoveryPointsExhaustedException {

        currentRecoveryPoint = getNextRecoveryPoint();

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
    TreeSet<RecoveryPointByBackupVault> getRecoveryPoints(String backupVaultName){

        if(recoveryPoints != null)
            return recoveryPoints;

        TreeSet<RecoveryPointByBackupVault> recoveryPointsTreeSet = new TreeSet<>( new RecoveryPointDateComparator());

        Instant nowMinusDay = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        nowMinusDay = nowMinusDay.minus(1, ChronoUnit.DAYS);

        //call and response with amazon to get list of vault backups
        ListRecoveryPointsByBackupVaultRequest request =
                ListRecoveryPointsByBackupVaultRequest.builder().
                        backupVaultName(backupVaultName).byResourceType(resourceType).byCreatedAfter(nowMinusDay).build();

        ListRecoveryPointsByBackupVaultResponse response =
                backupClient.listRecoveryPointsByBackupVault(request);

        recoveryPointsTreeSet.addAll(response.recoveryPoints());

        recoveryPointSetIterator = recoveryPointsTreeSet.iterator();

        return recoveryPointsTreeSet;
    }

    /**
     * Return most recent recovery point from vault.
     * @return a RecoveryPointByBackupVault
     * @throws RecoveryPointsExhaustedException when recovery points have been exhausted
     */

    synchronized RecoveryPointByBackupVault getNextRecoveryPoint() throws RecoveryPointsExhaustedException{

        if (!recoveryPointSetIterator.hasNext()){
            throw new RecoveryPointsExhaustedException("Recovery Points Exhausted");
        }

        return recoveryPointSetIterator.next();
    }

    static class RecoveryPointDateComparator implements Comparator<RecoveryPointByBackupVault> {
        @Override
        public int compare(RecoveryPointByBackupVault o1, RecoveryPointByBackupVault o2) {
            int result = o2.creationDate().compareTo(o1.creationDate());

            if(result == 0)
                result = o2.recoveryPointArn().compareTo(o1.recoveryPointArn());

            return result;
        }
    }

    public static class RecoveryPointsExhaustedException extends Exception{

        public RecoveryPointsExhaustedException(String message) {
            super(message);
        }
    }

    public static class InstanceUnavailableException extends Exception{

        public InstanceUnavailableException(String message) {
            super(message);
        }
        public InstanceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public RecoveryPointByBackupVault getCurrentRecoveryPoint() {
        return currentRecoveryPoint;
    }
}
