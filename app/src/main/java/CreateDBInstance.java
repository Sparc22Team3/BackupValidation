import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.CreateDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.CreateDbInstanceResponse;
import software.amazon.awssdk.services.rds.model.RdsException;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.DBInstance;
import java.util.List;

/** modified from:
 * https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/rds/src/main/java/com/example/rds/CreateDBInstance.java" */

public class CreateDBInstance {
    public static long sleepTime = 20;

    public static void main(String[] args) {

        final String usage = "\n" +
                "Usage:\n" +
                "    <dbInstanceIdentifier> <dbName> <masterUsername> <masterUserPassword> \n\n" +
                "Where:\n" +
                "    dbInstanceIdentifier - The database instance identifier. \n" +
                "    dbName - The database name. \n" +
                "    masterUsername - The master user name. \n" +
                "    masterUserPassword - The password that corresponds to the master user name. \n";

        if (args.length != 4) {
            System.out.println(usage);
            System.exit(1);
        }

        String dbInstanceIdentifier = "restoreDB";
        String dbName = args[1];
        String masterUsername = args[2];
        String masterUserPassword = args[3];

        Region region = Region.US_EAST_1;
        RdsClient rdsClient = RdsClient.builder()
                .region(region)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

        // createDatabaseInstance(rdsClient, dbInstanceIdentifier, dbName, masterUsername, masterUserPassword);
        // waitForInstanceReady(rdsClient, dbInstanceIdentifier);
        rdsClient.close();
    }

    // snippet-start:[rds.java2.create_instance.main]
    public static void createDatabaseInstance(RdsClient rdsClient,
                                              String dbInstanceIdentifier,
                                              String dbName,
                                              String masterUsername,
                                              String masterUserPassword) {

        try {
            CreateDbInstanceRequest instanceRequest = CreateDbInstanceRequest.builder()
                    .dbInstanceIdentifier(dbInstanceIdentifier)
                    .allocatedStorage(100)
                    .dbName(dbName)
                    .engine("mysql")
                    .dbInstanceClass("db.m4.large")
                    .engineVersion("8.0.15")
                    .storageType("standard")
                    .masterUsername(masterUsername)
                    .masterUserPassword(masterUserPassword)
                    .build();

            CreateDbInstanceResponse response = rdsClient.createDBInstance(instanceRequest);
            System.out.print("The status is " + response.dbInstance().dbInstanceStatus());

        } catch (RdsException e) {
            System.out.println(e.getLocalizedMessage());
            System.exit(1);
        }

    }

    // Waits until the database instance is available
    public static void waitForInstanceReady(RdsClient rdsClient, String dbInstanceIdentifier) {

        Boolean instanceReady = false;
        String instanceReadyStr = "";
        System.out.println("Waiting for instance to become available.");

        try {
            DescribeDbInstancesRequest instanceRequest = DescribeDbInstancesRequest.builder()
                    .dbInstanceIdentifier(dbInstanceIdentifier)
                    .build();

            // Loop until the cluster is ready
            while (!instanceReady) {

                DescribeDbInstancesResponse response = rdsClient.describeDBInstances(instanceRequest);
                List<DBInstance> instanceList = response.dbInstances();

                for (DBInstance instance : instanceList) {

                    instanceReadyStr = instance.dbInstanceStatus();
                    if (instanceReadyStr.contains("available"))
                        instanceReady = true;
                    else {
                        System.out.print(".");
                        Thread.sleep(sleepTime * 1000);
                    }
                }
            }
            System.out.println("Database instance is available!");

        } catch (RdsException | InterruptedException e) {

            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}