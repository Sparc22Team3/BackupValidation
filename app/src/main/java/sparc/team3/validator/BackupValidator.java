package sparc.team3.validator;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.s3.S3Client;
import sparc.team3.validator.restore.EC2Restore;
import sparc.team3.validator.restore.RDSRestore;
import sparc.team3.validator.restore.S3Restore;
import sparc.team3.validator.util.Settings;
import sparc.team3.validator.util.Util;
import sparc.team3.validator.validate.EC2ValidateInstance;
import sparc.team3.validator.validate.RDSValidate;
import sparc.team3.validator.validate.S3ValidateBucket;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Main class, sets up BackupValidator to run.
 */
public class BackupValidator {
    final Options options;
    final CommandLineParser parser;
    Configurator configurator;
    Settings settings;
    Logger logger;

    BackupClient backupClient;
    Ec2Client ec2Client;
    S3Client s3Client;
    RdsClient rdsClient;

    EC2Restore ec2Restore;
    S3Restore s3Restore;
    RDSRestore rdsRestore;

    Instance ec2Instance;
    String restoredBucketName;
    DBInstance rdsInstance;
    EC2ValidateInstance ec2ValidateInstance;
    RDSValidate rdsValidateDatabase;
    S3ValidateBucket s3ValidateBucket;

    /**
     * Constructs the BackupValidator class by setting up the command line options and parser.
     */
    private BackupValidator() {
        parser = new DefaultParser();
        options = new Options();
        options();


    }

    /**
     * Controls the flow of the program based on command line options.
     *
     * @param args the string array from the command line
     */
    private void run(String[] args) {
        try {
            CommandLine line = parser.parse(options, args);
            // Set logger values before loading logger
            if (line.hasOption("debug"))
                System.setProperty("log-level", "DEBUG");
            if (line.hasOption("trace"))
                System.setProperty("log-level", "TRACE");
            if (line.hasOption("log-file"))
                System.setProperty("log-file", line.getOptionValue("log-file"));

            logger = LoggerFactory.getLogger(BackupValidator.class);
            logger.info("Start - Arguments: {}", String.join(" ", args));

            if (line.hasOption("help")) {

                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(Util.APP_DIR_NAME, options);
                return;
            }
            if (line.hasOption("config"))
                configurator = new Configurator(line.getOptionValue("config"));
            else
                configurator = new Configurator();

            if (line.hasOption("newconfig")) {
                runNewConfig();
                return;
            }

            settings = configurator.loadSettings();

            // Spin up restored instances to get new hostnames/bucket name

            Configurator.replaceHostname("ec2_test", "rds_test", "s3_test", settings);

            logger.debug("Settings: {}", settings);

            Region region = Region.of(settings.getAwsRegion());
            backupClient = BackupClient.builder().region(region).build();
            ec2Client = Ec2Client.builder().region(region).build();
            s3Client = S3Client.builder().region(region).build();
            rdsClient = RdsClient.builder().region(region).build();

            restoreAndValidate();
            validateSystem();
            cleanUp();

        } catch (ParseException | IOException e) {
            printExceptionAndExit(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calls the configurator to output a blank config file
     */
    private void runNewConfig() {
        try {
            configurator.createBlankConfigFile();

        } catch (IOException e) {
            printException(e);
        }
    }

    /**
     * Uses a fixed thread pool from {@link Util#executor} to run the call methods
     * to restore and validate single instances and buckets.  Once this method is done,
     * EC2, RDS, and S3 will be restored and the instance/bucket will have been checked
     * to see if the instance is working.
     *
     * @throws InterruptedException
     */
    private void restoreAndValidate() throws InterruptedException {
        ec2Restore = new EC2Restore(backupClient, ec2Client, settings.getEc2Settings());
        s3Restore = new S3Restore(backupClient, s3Client, settings.getS3Settings());
        rdsRestore = new RDSRestore(rdsClient, settings.getRdsSettings());

        Future<Instance> ec2Future = Util.executor.submit(ec2Restore);
        Future<DBInstance> rdsFuture = Util.executor.submit(rdsRestore);
        Future<String> s3Future = Util.executor.submit(s3Restore);
        Future<Boolean> ec2ValidateFuture = null;
        Future<Boolean> rdsValidateFuture = null;
        Future<Boolean> s3ValidateFuture = null;

        boolean ec2Checked = false;
        boolean rdsChecked = false;
        boolean s3Checked = false;

        boolean ec2Passed = false;
        boolean rdsPassed = false;
        boolean s3Passed = false;

        // Stay in the loop until all three have been checked.
        while (!ec2Checked || !rdsChecked || !s3Checked) {
            /* Wait until the restore is done before attempting instance validation
             * Check if return value from future is set or not.  If it is,
             * this block has already been run on this result
             */
            if (ec2Future.isDone() && ec2Instance == null) {
                /* If an error occurs in the thread, future.get will throw an
                 * ExecutionException wrapped around the exception that was
                 * thrown in the thread
                 */
                try {
                    ec2Instance = ec2Future.get();
                    ec2ValidateInstance = new EC2ValidateInstance(ec2Client, settings.getEc2Settings());
                    ec2ValidateInstance.setEC2Instance(ec2Instance);

                    ec2ValidateFuture = Util.executor.submit(ec2ValidateInstance);
                } catch (ExecutionException e) {
                    logger.error("Restoring EC2 instance failed", e);
                    ec2Checked = true;
                }
            }

            if (rdsFuture.isDone() && rdsInstance == null) {
                try {
                    rdsInstance = rdsFuture.get();
                    rdsValidateDatabase = new RDSValidate(rdsClient, settings);
                    rdsValidateDatabase.setDbInstance(rdsInstance);

                    rdsValidateFuture = Util.executor.submit(rdsValidateDatabase);
                } catch (ExecutionException e) {
                    logger.error("Restoring RDS instance failed.", e);
                    rdsChecked = true;
                }
            }

            if (s3Future.isDone() && restoredBucketName == null) {
                try {
                    restoredBucketName = s3Future.get();
                    s3ValidateBucket = new S3ValidateBucket(s3Client, settings.getS3Settings());
                    s3ValidateBucket.setRestoredBucket(restoredBucketName);

                    s3ValidateFuture = Util.executor.submit(s3ValidateBucket);
                } catch (ExecutionException e) {
                    logger.error("Restoring S3 bucket failed.", e);
                    s3Checked = true;
                }

            }

            // If the validateFuture has been set then check if it is done
            // Don't run if its already been checked.
            if (!ec2Checked && ec2ValidateFuture != null && ec2ValidateFuture.isDone()) {
                try {
                    ec2Passed = ec2ValidateFuture.get();
                    if (ec2Passed) {
                        logger.info("EC2 Restored Instance Passed");

                    } else {
                        logger.info("EC2 Restored Instance Failed");
                    }
                } catch (ExecutionException e) {
                    logger.error("EC2 instance validation failed");
                }
                ec2Checked = true;
            }

            if (!rdsChecked && rdsValidateFuture != null && rdsValidateFuture.isDone()) {
                try {
                    rdsPassed = rdsValidateFuture.get();
                    if (rdsPassed) {
                        logger.info("Restored Database Passed");

                    } else {
                        logger.error("Restored Database Failed");
                    }
                } catch (ExecutionException e) {
                    logger.error("RDS database validation failed");
                }
                rdsChecked = true;
            }

            if (!s3Checked && s3ValidateFuture != null && s3ValidateFuture.isDone()) {
                try {
                    s3Passed = s3ValidateFuture.get();
                    if (s3Passed) {
                        logger.info("S3 Restored Bucket Passed");

                    } else {
                        logger.error("S3 Restored Bucket Failed");
                    }
                } catch (ExecutionException e) {
                    logger.error("S3 bucket validation failed");
                }
                s3Checked = true;
            }
            // Only run the loop once every minute.  None of our tasks are particularly quick.
            Thread.sleep(60000);
        }
    }

    /**
     * This method will validate that EC2, RDS, and S3 bucket are working as a whole system and
     * not just the individual pieces.
     */
    private void validateSystem() {
        logger.info("Do a System Check Here.");
    }

    private void cleanUp() {
        if (ec2Instance != null)
            Util.terminateEC2Instance(ec2Instance.instanceId(), ec2Client);
        if (rdsInstance != null)
            Util.deleteDBInstance(rdsInstance.dbInstanceIdentifier(), rdsClient);
        backupClient.close();
        ec2Client.close();
        s3Client.close();
        rdsClient.close();
        Util.executor.shutdown();
    }

    /**
     * Creates and adds options for the command line
     */
    private void options() {
        //options.addOption(new Option());
        options.addOption(new Option("h", "help", false, "print this message"));
        options.addOption(Option.builder("c")
                .longOpt("config")
                .hasArg().argName("file")
                .desc("configuration settings json file or directory containing json file for running "
                        + Util.APP_DISPLAY_NAME + ". If no config file is specified, program will look in " + Util.DEFAULT_CONFIG_DIR + " for " + Util.DEFAULT_CONFIG_FILENAME).build()
        );
        options.addOption(new Option("n", "newconfig", false, "create config template file at "
                + "location provided to config or at default location in " + Util.DEFAULT_CONFIG_DIR + " if config is not specified"));

        options.addOption(new Option("d", "debug", false, "change log level from INFO to DEBUG"));
        options.addOption(new Option("t", "trace", false, "change log level from INFO to TRACE"));
        options.addOption(Option.builder("lf")
                .longOpt("log-file")
                .hasArg().argName("file")
                .desc("change log file from default BackupValidator log file in " + System.getProperty("user.dir")).build());

    }

    /**
     * Formats and prints an exception message.
     *
     * @param e the exception to print message from
     */
    public static void printException(Exception e) {
        System.err.format("%s: %s\n", e.getClass().getSimpleName(), e.getMessage());
    }

    /**
     * Formats and prints an exception message before exiting the program.
     *
     * @param e the exception to print message from
     */
    public static void printExceptionAndExit(Exception e) {
        printException(e);
        System.exit(-1);
    }

    /**
     * Entry point for BackupValidator
     *
     * @param args the string array from the command line
     */
    public static void main(String[] args) {
        BackupValidator backupValidator = new BackupValidator();
        backupValidator.run(args);
    }
}
