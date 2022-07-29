package sparc.team3.validator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DeleteDbInstanceRequest;

import java.nio.file.Path;
import java.nio.file.Paths;
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
}
