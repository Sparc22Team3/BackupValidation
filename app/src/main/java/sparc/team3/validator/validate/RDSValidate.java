package sparc.team3.validator.validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DeleteDbInstanceRequest;

/**
 * This class tests and validates an RDS instance that was restored from a snapshot.
 */
public class RDSValidate {
    private final RdsClient rdsClient;
    private final DBInstance dbInstance;
    private final Logger logger;

    /**
     * Instantiates a new Rds validate.
     *
     * @param rdsClient     the rds client
     * @param dbInstance    the db instance representing the RDS DB instance
     */
    public RDSValidate(RdsClient rdsClient, DBInstance dbInstance) {
        this.rdsClient = rdsClient;
        this.dbInstance = dbInstance;
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
    }


    /**
     * Validate resource.
     *
     */
    public void validateResource() {
        logger.info("Validating restored database {}", dbInstance.dbInstanceIdentifier());
        deleteDBInstance(dbInstance.dbInstanceIdentifier());
    }

    /**
     * Delete database instance after testing and validating is complete.
     *
     * @param dbInstanceIdentifier the string of the rds instance id
     */
    private void deleteDBInstance(String dbInstanceIdentifier) {

        logger.info("Deleting database {}", dbInstanceIdentifier);
        DeleteDbInstanceRequest deleteRequest = DeleteDbInstanceRequest
                .builder()
                .dbInstanceIdentifier(dbInstanceIdentifier)
                .skipFinalSnapshot(true)
                .build();

        rdsClient.deleteDBInstance(deleteRequest);
    }
}
