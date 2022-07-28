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

import java.io.IOException;

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

    /**
     * Constructs the BackupValidator class by setting up the command line options and parser.
     */
    private BackupValidator(){
        parser = new DefaultParser();
        options = new Options();
        options();


    }

    /**
     * Controls the flow of the program based on command line options.
     * @param args the string array from the command line
     */
    private void run(String[] args) {
        try {
            CommandLine line = parser.parse(options, args);
            // Set logger values before loading logger
            if(line.hasOption("debug"))
                System.setProperty("log-level", "DEBUG");
            if(line.hasOption("trace"))
                System.setProperty("log-level", "TRACE");
            if(line.hasOption("log-file"))
                System.setProperty("log-file", line.getOptionValue("log-file"));

            logger = LoggerFactory.getLogger(BackupValidator.class);
            logger.info("Start - Arguments: {}", String.join(" ", args));

            if(line.hasOption("help")){

                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(Util.APP_DIR_NAME, options);
                return;
            }
            if(line.hasOption("config"))
                    configurator = new Configurator(line.getOptionValue("config"));
                else
                    configurator = new Configurator();

            if(line.hasOption("newconfig")){
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

            restore();
            validate();
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
    private void runNewConfig(){
        try {
            configurator.createBlankConfigFile();

        } catch (IOException e) {
            printException(e);
        }
    }

    private void restore() throws Exception {
        int ec2RecoveryAttempt = 0;
        EC2Restore ec2Restore = new EC2Restore(backupClient, ec2Client, settings.getEc2Settings(), ec2RecoveryAttempt);

        Instance ec2instance = ec2Restore.restoreEC2FromBackup();

        EC2ValidateInstance ec2ValidateInstance = new EC2ValidateInstance(ec2Client, ec2instance);

        Boolean validated = ec2ValidateInstance.validateWithPing("/wiki/index.php?title=Main_Page");

        System.out.println("Web Server Status 200: " + validated);

        ec2ValidateInstance.terminateEC2Instance();

        int s3RecoveryAttempt = 0;
        S3Restore s3Restore = new S3Restore(backupClient, s3Client, settings.getS3Settings());

        String bucketName = s3Restore.restoreS3FromBackup(s3RecoveryAttempt);

        System.out.println(bucketName);

        RDSRestore rdsRestore = new RDSRestore(rdsClient, settings.getRdsSettings());
        DBInstance rdsInstance = rdsRestore.restoreRDSFromBackup();

        RDSValidate rdsValidate = new RDSValidate(rdsClient, rdsInstance);
        rdsValidate.validateResource();

    }

    private void validate(){

    }

    private void cleanUp(){
        backupClient.close();
        ec2Client.close();
        s3Client.close();
        rdsClient.close();
    }

    /**
     * Creates and adds options for the command line
     */
    private void options(){
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

        options.addOption(new Option("d","debug", false, "change log level from INFO to DEBUG"));
        options.addOption(new Option("t", "trace",false, "change log level from INFO to TRACE"));
        options.addOption(Option.builder("lf")
                .longOpt("log-file")
                .hasArg().argName("file")
                .desc("change log file from default BackupValidator log file in " + System.getProperty("user.dir")).build());

    }

    /**
     * Formats and prints an exception message.
     * @param e the exception to print message from
     */
    public static void printException(Exception e){
        System.err.format("%s: %s\n", e.getClass().getSimpleName(), e.getMessage());
    }

    /**
     * Formats and prints an exception message before exiting the program.
     * @param e the exception to print message from
     */
    public static void printExceptionAndExit(Exception e){
        printException(e);
        System.exit(-1);
    }

    /**
     * Entry point for BackupValidator
     * @param args the string array from the command line
     */
    public static void main(String[] args) {
        BackupValidator backupValidator = new BackupValidator();
        backupValidator.run(args);
    }
}
