import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DeleteDbInstanceRequest;

/**
 * This class tests and validates an RDS instance that was restored from a snapshot.
 */
public class RDSValidate {
    private RdsClient rdsClient;
    private String uniqueNameForRestoredDBInstance;

    /**
     * Instantiates a new Rds validate.
     *
     * @param rdsClient                       the rds client
     * @param uniqueNameForRestoredDBInstance the unique name for restored db instance
     */
    public RDSValidate(RdsClient rdsClient, String uniqueNameForRestoredDBInstance) {
        this.rdsClient = rdsClient;
        this.uniqueNameForRestoredDBInstance = uniqueNameForRestoredDBInstance;
    }


    /**
     * Validate resource.
     *
     * @param dbInstanceIdentifier the db instance identifier
     */
    public void validateResource(String dbInstanceIdentifier) {

        deleteDBTestInstance(dbInstanceIdentifier);
    }

    /**
     * Delete database instance after testing and validating is complete.
     *
     * @param dbInstanceIdentifier
     */
    private void deleteDBTestInstance(String dbInstanceIdentifier) {

        System.out.println("in validate now, deleting..");
        DeleteDbInstanceRequest deleteRequest = DeleteDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .skipFinalSnapshot(true)
                .build();

        rdsClient.deleteDBInstance(deleteRequest);
    }
}
