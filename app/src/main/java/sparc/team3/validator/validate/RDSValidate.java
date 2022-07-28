package sparc.team3.validator.validate;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DeleteDbInstanceRequest;

/**
 * This class tests and validates an RDS instance that was restored from a snapshot.
 */
public class RDSValidate {
    private final RdsClient rdsClient;
    private final DBInstance dbInstance;

    /**
     * Instantiates a new Rds validate.
     *
     * @param rdsClient     the rds client
     * @param dbInstance    the db instance representing the RDS DB instance
     */
    public RDSValidate(RdsClient rdsClient, DBInstance dbInstance) {
        this.rdsClient = rdsClient;
        this.dbInstance = dbInstance;
    }


    /**
     * Validate resource.
     *
     */
    public void validateResource() {

        deleteDBInstance(dbInstance.dbInstanceIdentifier());
    }

    /**
     * Delete database instance after testing and validating is complete.
     *
     * @param dbInstanceIdentifier the string of the rds instance id
     */
    private void deleteDBInstance(String dbInstanceIdentifier) {

        System.out.println("in validate now, deleting..");
        DeleteDbInstanceRequest deleteRequest = DeleteDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .skipFinalSnapshot(true)
                .build();

        rdsClient.deleteDBInstance(deleteRequest);
    }
}
