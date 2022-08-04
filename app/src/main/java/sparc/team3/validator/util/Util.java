package sparc.team3.validator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DeleteDbInstanceRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default values and other useful tidbits.
 */
public class Util {
    /**
     * The name of the app is {@value}.
     */
    public static final String APP_DISPLAY_NAME = "Backup Validator";
    /**
     * The name of the app without spaces, used for directories and such, is {@value}.
     */
    public static final String APP_DIR_NAME = "BackupValidator";
    /**
     * The default config directory in the user's home directory.
     */
    public static final Path DEFAULT_CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".config", Util.APP_DIR_NAME);
    /**
     * The default config file name is {@value}.
     */
    public static final String DEFAULT_CONFIG_FILENAME = "config.json";

    public static final String UNIQUE_RESTORE_NAME_BASE = "restore-test-";

    public static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    /**
     * Terminate ec2Instance attached to client.
     */
    public static void terminateEC2Instance(String instanceId, Ec2Client ec2Client) {
        logger.info("Terminating Instance {}", instanceId);

        TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder().instanceIds(instanceId).build();
        ec2Client.terminateInstances(terminateRequest);

    }

    /**
     * Delete database instance after testing and validating is complete.
     *
     * @param dbInstanceIdentifier the string of the rds instance id
     */
    public static void deleteDBInstance(String dbInstanceIdentifier, RdsClient rdsClient) {

        logger.info("Deleting database {}", dbInstanceIdentifier);
        DeleteDbInstanceRequest deleteRequest = DeleteDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .skipFinalSnapshot(true)
                .build();

        rdsClient.deleteDBInstance(deleteRequest);
    }

    /**
     * Delete restored S3 bucket instance after testing and validation is complete.
     * @param bucketName the string of the restored S3 bucket
     * @param s3Client to be used to initiate the bucket deletion
     */
    public static void deleteS3Instance(String bucketName, S3Client s3Client) {

        System.out.println("Beginning deletion process for S3 Bucket: " + bucketName);

        // initialize variables
        ListObjectVersionsRequest listVerions = ListObjectVersionsRequest.builder().bucket(bucketName).build();
        ListObjectVersionsResponse response;
        List<ObjectIdentifier> objectsToBeDeleted = new ArrayList<>();
        ObjectIdentifier objectId;
        String key;
        String versionId;

        // step 1: delete all existing objects (including ones with delete markers)

        // 1a: get all ObjectVersions
        do {
            response = s3Client.listObjectVersions(listVerions);

            // 1b: check to see if there are any objects with delete markers
            if (!response.deleteMarkers().isEmpty()) {
                // there are deleted objects to be deleted
                List<DeleteMarkerEntry> deleteEntries = response.deleteMarkers();

                // clear out objectsToBeDeleted to store new objects
                objectsToBeDeleted.clear();

                for (DeleteMarkerEntry entry : deleteEntries) {
                    // obtain object key and version id of the object
                    key = entry.key();
                    versionId = entry.versionId();

                    // use key and version id to initialize ObjectIdentifier
                    objectId = ObjectIdentifier.builder().key(key).versionId(versionId).build();

                    // add objectId to objectsToBeDeleted
                    objectsToBeDeleted.add(objectId);
                }

                // 1c: delete remaining objects
            } else {
                // get the remaining objects that are not deleted
                List<ObjectVersion> existingObjects = response.versions();

                // clear out objectsToBeDeleted
                objectsToBeDeleted.clear();

                // iterate through list and add object info to list
                for (ObjectVersion object : existingObjects) {
                    key = object.key();
                    versionId = object.versionId();
                    objectId = ObjectIdentifier.builder().key(key).versionId(versionId).build();
                    objectsToBeDeleted.add(objectId);
                }
            }

            // 1d: delete multiple objects in one request by passing in objectsToBeDeleted
            Delete del = Delete.builder().objects(objectsToBeDeleted).build();

            try {
                DeleteObjectsRequest multiObjDeleteRequest = DeleteObjectsRequest.builder().bucket(bucketName).delete(del).build();
                s3Client.deleteObjects(multiObjDeleteRequest);
            } catch (S3Exception e) {
                System.err.println(e.awsErrorDetails().errorMessage());
                System.exit(1);
            }

            // isTruncated() will return false if all results are returned
        } while(response.isTruncated());

        // step 2: delete bucket after it has been emptied
        try {
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest
                    .builder()
                    .bucket(bucketName)
                    .build();
            s3Client.deleteBucket(deleteBucketRequest);
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

        System.out.println("S3 Bucket: " + bucketName + " is deleted.");
    }
}
