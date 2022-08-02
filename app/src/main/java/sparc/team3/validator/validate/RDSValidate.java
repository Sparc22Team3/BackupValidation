package sparc.team3.validator.validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import sparc.team3.validator.util.InstanceSettings;
import sparc.team3.validator.util.Settings;

import java.util.concurrent.Callable;

/**
 * This class tests and validates an RDS instance that was restored from a snapshot.
 */
public class RDSValidate implements Callable<Boolean> {
    private final RdsClient rdsClient;
    private final Settings settings;
    private final Logger logger;
    private DBInstance dbInstance;

    /**
     * Instantiates a new Rds validate.
     *
     * @param rdsClient     the rds client
     */
    public RDSValidate(RdsClient rdsClient, Settings settings) {
        this.rdsClient = rdsClient;
        this.settings = settings;
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
    }

    public void setDbInstance(DBInstance dbInstance){
        this.dbInstance = dbInstance;
    }

    @Override
    public Boolean call() {
        validateResource();
        return true;
    }
    /**
     * Validate resource.
     *
     */
    public void validateResource() {
        logger.info("Validating restored database {}", dbInstance.dbInstanceIdentifier());
    }
}
