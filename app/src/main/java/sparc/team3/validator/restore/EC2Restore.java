package sparc.team3.validator.restore;

import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.backup.model.*;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;
import sparc.team3.validator.config.settings.InstanceSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

/**
 * sparc.team3.validator.restore.EC2Restore selects a recovery point to back up, restores the recovery point, and waits for the
 * recovered resource to pass initialization checks. 
 */
public class EC2Restore extends AWSRestore implements Callable<Instance> {


    private final Ec2Client ec2Client;

    public EC2Restore(BackupClient backupClient, Ec2Client ec2Client, InstanceSettings instanceSettings) {
        super(backupClient, instanceSettings, "EC2");
        this.ec2Client = ec2Client;
    }

    @Override
    public Instance call() throws InstanceUnavailableException, RecoveryPointsExhaustedException {
        return restoreEC2FromBackup();
    }
    /**
     * Polls AWS Backup to check when restore job is complete. Returns error if restore job took
     * longer than 10 minutes.
     * Throws error if job isn't completed within allotted time.
     * @return a string of the instance id
     * @throws InterruptedException when sleep is interrupted
     */
    public Instance restoreEC2FromBackup() throws InstanceUnavailableException, RecoveryPointsExhaustedException {

        String restoreJobId = startRestore();
        if(restoreJobId == null)
            return null;
        int sleepMinutes = 1;
        String finalStatus = null;
        Arn resourceArn = null;

        logger.info("Restore EC2 job: {}", restoreJobId);
        while (finalStatus == null) {
            try {
                //get restore job information and wait until status of restore job is "completed"
                DescribeRestoreJobRequest newRequest = DescribeRestoreJobRequest
                        .builder().restoreJobId(restoreJobId).build();
                DescribeRestoreJobResponse restoreResult = backupClient.describeRestoreJob(newRequest);
                String status = restoreResult.status().toString();

                logger.info("Restore Job {}\t\tStatus: {}\t\tPercent Done: {}", restoreJobId,
                        status, restoreResult.percentDone());

                // when restore job is no longer running, break out of the loop otherwise sleep for a while
                if (!status.equals("RUNNING") && !status.equals("PENDING")) {
                    finalStatus = status;
                    resourceArn = Arn.fromString(restoreResult.createdResourceArn());
                } else {
                    Thread.sleep(sleepMinutes * 60000);
                }
            } catch (BackupException | InterruptedException e) {
                logger.error("Problem with restore job id: {}", restoreJobId, e);
                return null;
            }
        }

        if (!finalStatus.equals("COMPLETED")){
            logger.error("Restore job {} did not complete.", restoreJobId);
            return null;
        }

        Ec2Waiter waiter = ec2Client.waiter();

        String id = resourceArn.resource().resource();

        logger.info("Waiting for ec2 instance {} to be available", id);
        DescribeInstanceStatusRequest statusReq = DescribeInstanceStatusRequest
                .builder().instanceIds(id).build();

        ResponseOrException<DescribeInstanceStatusResponse> responseOrException = waiter.waitUntilInstanceStatusOk(statusReq).matched();

        if(responseOrException.response().isPresent()){
            return getInstance(id);
        }
        else if(responseOrException.exception().isPresent())
            throw new InstanceUnavailableException("Exception was returned instead of EC2 Instance", responseOrException.exception().get());
        else
            throw new InstanceUnavailableException("Exception was returned instead of EC2 Instance");
    }

    /**
     * Set the metadata required for restore
     * @return Map of the metadata
     */
    Map<String, String> setMetadata(){
        return editRecoveryMeta(getRecoveryMetaData(currentRecoveryPoint));
    }

    /**
     * Gets metadata of recovery point for restore job request.
     * @param recoveryPoint the RecoveryPointByBackupVault to get metadata from
     * @return a Map of the string metadata
     */
    private  Map<String, String> getRecoveryMetaData(RecoveryPointByBackupVault recoveryPoint){

        Map<String, String> output;

        GetRecoveryPointRestoreMetadataRequest request = 
        GetRecoveryPointRestoreMetadataRequest.builder().
        recoveryPointArn(recoveryPoint.recoveryPointArn()).backupVaultName(instanceSettings.getBackupVault()).build();
        
        GetRecoveryPointRestoreMetadataResponse response = backupClient
        .getRecoveryPointRestoreMetadata(request);

        //Modify network settings
        output = response.restoreMetadata(); //this becomes immutable? Remove gets angry.

        return output; 
        
    }

    /**
     * Solves issue with metadata where restore job request does not work with
     * NetworkInterfaces if security group and subnet are specified.
     * Also, CpuOptions causes restore job request to fail.
     * 
     * @param metaData a Map of string metadata
     * @return the edited meta data Map
     */

    private Map<String, String> editRecoveryMeta(Map<String, String> metaData){

        Map<String, String> output = new HashMap<>();

        for(Entry<String, String> entry: metaData.entrySet()){

            if(!entry.getKey().equals("NetworkInterfaces")){

                if(entry.getKey().equals("CpuOptions")){continue;}

                output.put(entry.getKey(), entry.getValue()); 
            }

        }

        return output; 
    }

    /**
     * Get the Instance information about the EC2 instance that was restored
     *
     * @param id the id of the instance we want to get
     * @return an Instance describing the instance that was restored
     */
    private Instance getInstance(String id) {
        DescribeInstancesRequest instanceReq = DescribeInstancesRequest.builder().instanceIds(id).build();

        DescribeInstancesResponse descriptionRes = ec2Client.describeInstances(instanceReq);

        // Get first reservation in a list of reservations that should only have 1
        Reservation reservation = descriptionRes.reservations().get(0);
        if(descriptionRes.reservations().size() > 1)
            logger.warn("More than 1 reservation was returned");
        // Get first instance in list of instances that should only have 1
        Instance instance = reservation.instances().get(0);
        if(reservation.instances().size() > 1)
            logger.warn("More than 1 instance was returned");

        return instance;
    }
}