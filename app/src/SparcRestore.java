package com.example.myapp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import software.amazon.awssdk.services.backup.BackupClient;

import software.amazon.awssdk.services.backup.model.GetRecoveryPointRestoreMetadataRequest;
import software.amazon.awssdk.services.backup.model.GetRecoveryPointRestoreMetadataResponse;
import software.amazon.awssdk.services.backup.model.ListRecoveryPointsByBackupVaultRequest;
import software.amazon.awssdk.services.backup.model.ListRecoveryPointsByBackupVaultResponse;
import software.amazon.awssdk.services.backup.model.RecoveryPointByBackupVault;
import software.amazon.awssdk.services.backup.model.StartRestoreJobRequest;
import software.amazon.awssdk.services.backup.model.StartRestoreJobResponse;
import software.amazon.awssdk.services.ec2.Ec2Client;

/**
 * Class used to initiate restore of most recent EC2
 */
public class SparcRestore {

    BackupClient client; 
    Ec2Client ec2client; 

    public SparcRestore(BackupClient client, Ec2Client e2client){

        this.client = client; 
        this.ec2client = e2client; 

    }
    
    /**
     * Method takes in the vault name and the desired backup number and returns the backup information for restore. 
     * @param backupVaultName
     * @param targetBackup //0-11 where 0 is the most recent backup.
     * @return
     * 
     * TO DO: ADD FUNCTIONALITY TO CHOOSE ANY BACKUP
     */
    public RecoveryPointByBackupVault getMostRecentRecoveryPoint(String backupVaultName, int mostRecentBackup){

        //call and response with amazon to get list of vault backups
        ListRecoveryPointsByBackupVaultRequest  request = ListRecoveryPointsByBackupVaultRequest.builder().backupVaultName(backupVaultName).build();
        ListRecoveryPointsByBackupVaultResponse response = client.listRecoveryPointsByBackupVault(request); 

        //convert string to datetime. Set max to initial value and iterate through rest of list
        Instant mostRecent = null;
        software.amazon.awssdk.services.backup.model.RecoveryPointByBackupVault instance = null; 

        //NEED TO EXTEND TO CHOOSE ANY OF THE 24 backups
        for(software.amazon.awssdk.services.backup.model.RecoveryPointByBackupVault r: response.recoveryPoints()){

            if (mostRecent == null){

                mostRecent = r.completionDate(); 
                instance = r; 
            }

            else{

                if(mostRecent.compareTo(r.completionDate()) < 0){

                    mostRecent = r.completionDate();
                    instance = r; 
                }
            }
        }

        return instance; 
    }

    /**
     * Given a recovery point, the function initates the recovery given a recovery point. 
     * @param recoveryPoint
     * @return
     */
    public String restoreResource(String backupVaultName, int mostRecentBackup){

        RecoveryPointByBackupVault recoveryPoint = getMostRecentRecoveryPoint(backupVaultName, 0);
        Map<String, String> raw = getRecoveryMeta(recoveryPoint, backupVaultName);
        Map<String, String> metadata = editRecoveryMeta(raw);
        
        StartRestoreJobRequest request = StartRestoreJobRequest.builder().
        recoveryPointArn(recoveryPoint.recoveryPointArn()).iamRoleArn(recoveryPoint.iamRoleArn()).metadata(metadata).build();
        StartRestoreJobResponse response = client.startRestoreJob(request); 

        return response.restoreJobId(); 

    }


    public  Map<String, String> getRecoveryMeta(RecoveryPointByBackupVault recoveryPoint, String backupVaultName){

        Map<String, String> output;

        GetRecoveryPointRestoreMetadataRequest request = GetRecoveryPointRestoreMetadataRequest.builder().
        recoveryPointArn(recoveryPoint.recoveryPointArn()).backupVaultName(backupVaultName).build();
        GetRecoveryPointRestoreMetadataResponse response = client.getRecoveryPointRestoreMetadata(request);

        //Modify network settings
        output = response.restoreMetadata();

        return output; 
        
    }

    public Map<String, String> editRecoveryMeta(Map<String, String> metaData){

        Map<String, String> output = new HashMap<String, String>(); 

        for(Entry<String, String> entry: metaData.entrySet()){

            if(entry.getKey() != "NetworkInterfaces"){

                if(entry.getKey() == "CpuOptions"){continue;}

                output.put(entry.getKey(), entry.getValue()); 
            }

            else{

                //output.put("NetworkInterfaces", "[{\"DeleteOnTermination\":true,\"Description\":\"\",\"DeviceIndex\":0,\"Groups\":[\"sg-0604682d854c1826a\"],\"Ipv6AddressCount\":0,\"Ipv6Addresses\":[],\"PrivateIpAddresses\":[{\"Primary\":true,\"PrivateIpAddress\":\"10.0.4.249\"}],\"SecondaryPrivateIpAddressCount\":1,\"SubnetId\":\"subnet-0c36c936e9f92c98c\",\"InterfaceType\":\"interface\"}]");
                continue;

            }
            
        }

        return output; 

    }
    
}