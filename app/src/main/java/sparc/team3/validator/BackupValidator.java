package sparc.team3.validator;

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.backup.BackupClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import sparc.team3.validator.config.ConfigEditor;
import sparc.team3.validator.config.ConfigLoader;
import sparc.team3.validator.config.SeleniumEditor;
import sparc.team3.validator.config.SeleniumLoader;
import sparc.team3.validator.restore.*;
import sparc.team3.validator.util.CLI;
import sparc.team3.validator.config.settings.Settings;
import sparc.team3.validator.util.Notification;
import sparc.team3.validator.util.RemoteServerConfigurator;
import sparc.team3.validator.util.Util;
import sparc.team3.validator.config.seleniumsettings.SeleniumSettings;
import sparc.team3.validator.validate.EC2ValidateInstance;
import sparc.team3.validator.validate.RDSValidate;
import sparc.team3.validator.validate.S3ValidateBucket;
import sparc.team3.validator.validate.WebAppValidate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Main class, sets up BackupValidator to run.
 */
public class BackupValidator {
    final Options options;
    final CommandLineParser parser;
    final CLI cli;
    ConfigLoader configLoader;
    Settings settings;
    Path saveLocation;
    SeleniumSettings seleniumSettings;
    Logger logger;
    BackupClient backupClient;
    Ec2Client ec2Client;
    S3Client s3Client;
    RdsClient rdsClient;
    SnsClient snsClient;
    EC2Restore ec2Restore;
    S3Restore s3Restore;
    RDSRestore rdsRestore;
    Instance ec2Instance;
    String restoredBucketName;
    DBInstance rdsInstance;
    EC2ValidateInstance ec2ValidateInstance;
    RDSValidate rdsValidateDatabase;
    S3ValidateBucket s3ValidateBucket;
    private Boolean ec2Passed = false;
    private Boolean rdsPassed = false;
    private Boolean s3Passed = false;
    private Boolean systemPassed = null;
    private Map<String, String> loadedResourcesMap;

    /**
     * Constructs the BackupValidator class by setting up the command line options and parser.
     */
    private BackupValidator() {
        parser = new DefaultParser();
        options = new Options();
        options();
        cli = new CLI();


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
                .desc("Configuration Location: configuration settings json file or directory containing json file for running "
                        + Util.APP_DISPLAY_NAME + ". If no config file is specified, program will look in " + Util.DEFAULT_CONFIG_DIR + " for " + Util.DEFAULT_CONFIG_FILENAME).build()
        );
        options.addOption(Option.builder("s")
                .longOpt("selenium")
                .hasArg().argName("file")
                .desc(" Selenium Configuration Location: selenium settings json file or directory containing json file for running " +
                        "selenium. If no config file is specified, program will look in " + Util.DEFAULT_CONFIG_DIR + " for " + Util.DEFAULT_SELENIUM_FILENAME).build()
        );
        options.addOption(new Option("n", "newconfig", false, "New Config File: create config file at "
                + "location provided to '--config' or at default location in " + Util.DEFAULT_CONFIG_DIR + " if config is not specified"));
        options.addOption(new Option("m", "modifyconfig", false, "Modify Config File: modify config file at "
                + "location provided to '--config' or at default location in " + Util.DEFAULT_CONFIG_DIR + " if config is not specified"));
        options.addOption(new Option("ns", "newselenium", false, "New Selenium File: create selenium config file at "
                + "location provided to '--selenium' or at default location in " + Util.DEFAULT_CONFIG_DIR + " if selenium is not specified"));
        options.addOption(new Option("ms", "modifyselenium", false, "Modify Selenium File: modify selenium file at "
                + "location provided to '--selenium' or at default location in " + Util.DEFAULT_CONFIG_DIR + " if selenium is not specified"));

        options.addOption(new Option("d", "debug", false, "Debug: change log level from INFO to DEBUG"));
        options.addOption(new Option("t", "trace", false, "Trace: change log level from INFO to TRACE"));
        options.addOption(Option.builder("lf")
                .longOpt("log-file")
                .hasArg().argName("file")
                .desc("Log File: change log file from default BackupValidator log file in " + System.getProperty("user.dir")).build());

        options.addOption(new Option("xr", "donotrestore", false, "Do not restore.  Will use saved instance information to get previously restored instances"));
        options.addOption(new Option("xt", "donotterminate", false, "Do not terminate.  Will save instance information of restored instances to be used later."));
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

            String configFile = line.getOptionValue("config", Util.DEFAULT_CONFIG.toString());
            ConfigEditor configEditor = new ConfigEditor(cli, configFile);
            if (line.hasOption("newconfig")) {
                configEditor.runBuilder();
                return;
            } else if (line.hasOption("modifyconfig")) {
                configEditor.runEditor();
                return;
            }

            String seleniumFile = line.getOptionValue("selenium", Util.DEFAULT_SELENIUM.toString());
            SeleniumEditor seleniumEditor = new SeleniumEditor(cli, seleniumFile);
            if(line.hasOption("newselenium")){
                seleniumEditor.runBuilder();
                return;
            } else if (line.hasOption("modifyselenium")) {
                seleniumEditor.runEditor();
                return;
            }
            configLoader = new ConfigLoader(cli, configFile);
            SeleniumLoader seleniumLoader = new SeleniumLoader(cli, seleniumFile);

            try {
                seleniumSettings = seleniumLoader.loadSettings();
                settings = configLoader.loadSettings();
            } catch (IOException e){
                logger.error("Missing config files: {}", e.getMessage());
                return;
            }

            if (settings == null)
                return;
            saveLocation = configLoader.getActualConfigDir();
            saveLocation = saveLocation.resolve("save.json");

            logger.debug("Settings: {}", settings);

            Region region = Region.of(settings.getAwsRegion());
            backupClient = BackupClient.builder().region(region).build();
            ec2Client = Ec2Client.builder().region(region).build();
            s3Client = S3Client.builder().region(region).build();
            rdsClient = RdsClient.builder().region(region).build();
            snsClient = SnsClient.builder().region(region).build();

            boolean restore = !line.hasOption("donotrestore");
            boolean terminate = !line.hasOption("donotterminate");

            boolean pass = restoreAndValidate(restore);

            saveInstances();
            if(pass)
                systemPassed = validateSystem();

            cleanUp(terminate);
            report();

        } catch (Exception e) {
            logger.error("{}: Instances were not terminated.", e.getMessage(), e);
            try {
                cleanUp(false);
                report(e);
            } catch (InterruptedException | IOException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
    }


    /**
     * Uses a fixed thread pool from {@link Util#executor} to run the call methods
     * to restore and validate single instances and buckets.  Once this method is done,
     * EC2, RDS, and S3 will be restored and the instance/bucket will have been checked
     * to see if the instance is working.
     *
     * @throws InterruptedException when another thread interrupts this one
     */
    private boolean restoreAndValidate(boolean restore) throws InterruptedException, IOException {
        Future<Instance> ec2Future = null;
        Future<DBInstance> rdsFuture = null;
        Future<String> s3Future = null;

        Future<Boolean> ec2ValidateFuture = null;
        Future<Boolean> rdsValidateFuture = null;
        Future<Boolean> s3ValidateFuture = null;

        ec2ValidateInstance = new EC2ValidateInstance(ec2Client, settings.getEc2Settings());
        rdsValidateDatabase = new RDSValidate(rdsClient, settings.getRdsSettings());
        rdsValidateDatabase.setDBCredentials(settings.getDbUsername(), settings.getDbPassword());
        rdsValidateDatabase.setDatabasesToCheck(settings.getDatabases());
        s3ValidateBucket = new S3ValidateBucket(s3Client, settings.getS3Settings());

        boolean ec2Checked = false;
        boolean rdsChecked = false;
        boolean s3Checked = false;

        // If we are restoring start the restore threads and then enter the loop below to wait for them to complete
        if(restore) {
            ec2Restore = new EC2Restore(backupClient, ec2Client, settings.getEc2Settings());
            s3Restore = new S3Restore(backupClient, s3Client, settings.getS3Settings());
            rdsRestore = new RDSRestore(backupClient, rdsClient, settings.getRdsSettings());

            ec2Future = Util.executor.submit(ec2Restore);
            rdsFuture = Util.executor.submit(rdsRestore);
            s3Future = Util.executor.submit(s3Restore);
        }
        // If we are not restoring, load the previous instances and start validating, then enter loop waiting for them to complete
        else {
            loadInstances();

            ec2ValidateInstance.setEC2Instance(ec2Instance);
            ec2ValidateFuture = Util.executor.submit(ec2ValidateInstance);

            rdsValidateDatabase.setRestoredDbInstance(rdsInstance);
            rdsValidateFuture = Util.executor.submit(rdsValidateDatabase);

            s3ValidateBucket.setRestoredBucket(restoredBucketName);
            s3ValidateFuture = Util.executor.submit(s3ValidateBucket);
        }

        // Stay in the loop until all three have been checked.
        while (!ec2Checked || !rdsChecked || !s3Checked) {
            // Only run the loop once every minute.  None of our tasks are particularly quick.
            Thread.sleep(60000);
            /* Wait until the restore is done before attempting instance validation
             * Check if return value from future is set or not.  If it is,
             * this block has already been run on this result
             */
            if (ec2Future != null && ec2Future.isDone() && ec2Instance == null && !ec2Checked) {
                /* If an error occurs in the thread, future.get will throw an
                 * ExecutionException wrapped around the exception that was
                 * thrown in the thread
                 */
                try {
                    ec2Instance = ec2Future.get();
                    ec2ValidateInstance.setEC2Instance(ec2Instance);

                    ec2ValidateFuture = Util.executor.submit(ec2ValidateInstance);
                } catch (ExecutionException e) {
                    logger.error("Restoring EC2 instance failed. Cause: {}", e.getMessage());
                    ec2Checked = true;
                }
            }

            if (rdsFuture != null && rdsFuture.isDone() && rdsInstance == null && !rdsChecked) {
                try {
                    rdsInstance = rdsFuture.get();
                    rdsValidateDatabase.setRestoredDbInstance(rdsInstance);
                    rdsValidateFuture = Util.executor.submit(rdsValidateDatabase);
                } catch (ExecutionException e) {
                    logger.error("Restoring RDS instance failed. Cause: {}", e.getMessage());
                    rdsChecked = true;
                }
            }

            if (s3Future != null && s3Future.isDone() && restoredBucketName == null && !s3Checked) {
                try {
                    restoredBucketName = s3Future.get();
                    s3ValidateBucket.setRestoredBucket(restoredBucketName);
                    s3ValidateFuture = Util.executor.submit(s3ValidateBucket);
                } catch (ExecutionException e) {
                    logger.error("Restoring S3 bucket failed. Cause: {}", e.getMessage());
                    s3Checked = true;
                }

            }

            // If the validateFuture has been set then check if it is done
            // Don't run if its already been checked.
            if (!ec2Checked && ec2ValidateFuture != null && ec2ValidateFuture.isDone()) {
                try {
                    ec2Passed = ec2ValidateFuture.get();
                    if (ec2Passed) {
                        logger.info("EC2 Restored Instance Validation Passed");

                    } else {
                        logger.info("EC2 Restored Instance Validation Failed");
                    }
                } catch (ExecutionException e) {
                    logger.error("EC2 instance validation failed. Cause: {}", e.getMessage());
                }
                ec2Checked = true;
            }

            if (!rdsChecked && rdsValidateFuture != null && rdsValidateFuture.isDone()) {
                try {
                    rdsPassed = rdsValidateFuture.get();
                    if (rdsPassed) {
                        logger.info("Restored Database Validation Passed");

                    } else {
                        logger.info("Restored Database Validation Failed");
                    }
                } catch (ExecutionException e) {
                    logger.error("RDS database validation failed. Cause: {}", e.getMessage());
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
                    logger.error("S3 bucket validation failed. Cause: {}", e.getMessage());
                }
                s3Checked = true;
            }
        }
        return ec2Passed && rdsPassed && s3Passed;
    }

    /**
     * This method will validate that EC2, RDS, and S3 bucket are working as a whole system and
     * not just the individual pieces.
     */
    private boolean validateSystem() throws Exception {
        // Spin up restored instances to get new hostnames/bucket name
        ConfigLoader.replaceHostname(ec2Instance.publicDnsName(), rdsInstance.endpoint().address(), restoredBucketName, settings);
        if(!System.getProperty("user.name").startsWith("ec2"))
            new RemoteServerConfigurator(ec2Instance.publicIpAddress(), settings);
        else
            new RemoteServerConfigurator(ec2Instance.privateIpAddress(), settings);

        WebAppValidate webAppValidate = new WebAppValidate(ec2Instance, seleniumSettings);

        boolean webAppPass = webAppValidate.call();
        if(webAppPass)
            logger.info("Web App has passed Selenium validation tests.");
        else
            logger.warn("Web App has failed Selenium validation tests.");

        return webAppPass;
    }

    /**
     * If the user wishes to terminate the restored instances, terminates/deletes those instances.
     * Closes clients and shutsdown executor
     * @param terminate boolean whether to terminate restored instances
     * @throws IOException if there is an IO error
     * @throws InterruptedException if executor is interrupted
     */
    private void cleanUp(boolean terminate) throws IOException, InterruptedException {
        if(terminate) {
            if (ec2Instance != null)
                Util.terminateEC2Instance(ec2Instance.instanceId(), ec2Client);
            if (rdsInstance != null)
                Util.deleteDBInstance(rdsInstance.dbInstanceIdentifier(), rdsClient);
            if (restoredBucketName != null)
                Util.deleteS3Instance(restoredBucketName, s3Client);
            if(Files.exists(saveLocation))
                Files.delete(saveLocation);
        }
        backupClient.close();
        ec2Client.close();
        s3Client.close();
        rdsClient.close();
        Util.executor.shutdown();
    }

    /**
     * Save information about instances restored and the recovery points used to restore them.
     * @throws IOException if there is an IO error saving the file.
     */
    private void saveInstances() throws IOException {
        Map<String, String> saveResourcesMap = new HashMap<>();
        saveResourcesMap.put("EC2", ec2Instance.instanceId());
        saveResourcesMap.put("RDS", rdsInstance.dbInstanceIdentifier());
        saveResourcesMap.put("S3", restoredBucketName);

        String ec2RecoveryPoint = ec2Restore == null ? loadedResourcesMap.get("EC2RecoveryPoint") : ec2Restore.getCurrentRecoveryPoint().recoveryPointArn();
        String rdsRecoveryPoint = rdsRestore == null ? loadedResourcesMap.get("RDSRecoveryPoint") : rdsRestore.getCurrentRecoveryPoint().recoveryPointArn();
        String s3RecoveryPoint = s3Restore == null ? loadedResourcesMap.get("S3RecoveryPoint") : s3Restore.getCurrentRecoveryPoint().recoveryPointArn();

        saveResourcesMap.put("EC2RecoveryPoint", ec2RecoveryPoint);
        saveResourcesMap.put("RDSRecoveryPoint", rdsRecoveryPoint);
        saveResourcesMap.put("S3RecoveryPoint", s3RecoveryPoint);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(saveLocation.toFile(), saveResourcesMap);
    }

    /**
     * Loads information about previously restored but not terminated instances and the recovery points used to restore them.
     * @throws IOException
     */
    private void loadInstances() throws IOException {
        TypeReference<HashMap<String, String>> typeReference = new TypeReference<HashMap<String, String>>(){};

        if(!Files.exists(saveLocation))
            logger.error("Save file 'save.json' does not exist in {} to load.  Please load a config file from the same " +
                    "location as your save file or run {} without the 'donotrestore' option",
                    saveLocation.getParent(), Util.APP_DISPLAY_NAME);

        ObjectMapper mapper = new ObjectMapper();

        loadedResourcesMap = mapper.readValue(saveLocation.toFile(), typeReference);

        // Set S3 restored bucket name
        restoredBucketName = loadedResourcesMap.get("S3");
        if(restoredBucketName.isEmpty())
            throw new IOException("Missing restored bucket name in save file.");

        String ec2InstanceID = loadedResourcesMap.get("EC2");

        if(ec2InstanceID == null || ec2InstanceID.isEmpty())
            throw new IOException("Missing restored EC2 instance-id in save file.");

        String dbIdentifier = loadedResourcesMap.get("RDS");

        if(dbIdentifier == null || dbIdentifier.isEmpty())
            throw new IOException("Missing restored DB identifier in save file.");

        // Use the saved EC2 instance-id to get the Instance back from AWS
        DescribeInstancesRequest.Builder describeInstancesRequest  = DescribeInstancesRequest.builder().instanceIds(ec2InstanceID);
        DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(describeInstancesRequest.build());
        ec2Instance = describeInstancesResponse.reservations().get(0).instances().get(0);

        // Use the saved DB instance identifier to get the
        DescribeDbInstancesRequest.Builder describeDbInstancesRequest = DescribeDbInstancesRequest.builder().dbInstanceIdentifier(dbIdentifier);
        DescribeDbInstancesResponse describeDbInstances = rdsClient.describeDBInstances(describeDbInstancesRequest.build());
        rdsInstance = describeDbInstances.dbInstances().get(0);

    }

    /**
     * Build and send the final report of what has passed and any tests failed during testing.
     * @param thrown Exception that may need to be included in report
     * @throws IOException if there is an IO error
     */
    private void report(Throwable thrown) throws IOException {

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Path file = Paths.get(context.getProperty("reportFileName"));
        HashMap<String, List<String>> sortedReportItems = new HashMap<>();
        boolean passed = false;
        StringBuilder stringBuilder = new StringBuilder();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd_HH-mm"));


        stringBuilder
                .append(String.format("%s Report %s\n\n", Util.APP_DISPLAY_NAME, date));

        if(thrown == null) {
            String ec2RecoveryPoint = ec2Restore == null ? loadedResourcesMap.getOrDefault("EC2RecoveryPoint", "Missing Recovery Point Arn") : ec2Restore.getCurrentRecoveryPoint().recoveryPointArn();
            String rdsRecoveryPoint = rdsRestore == null ? loadedResourcesMap.getOrDefault("RDSRecoveryPoint", "Missing Recovery Point Arn") : rdsRestore.getCurrentRecoveryPoint().recoveryPointArn();
            String s3RecoveryPoint = s3Restore == null ? loadedResourcesMap.getOrDefault("S3RecoveryPoint", "Missing Recovery Point Arn") : s3Restore.getCurrentRecoveryPoint().recoveryPointArn();

            stringBuilder
                    .append(String.format("EC2 Recovery Point: %s\n\tPassed Tests: %s\n", ec2RecoveryPoint, ec2Passed))
                    .append(String.format("RDS Recovery Point: %s\n\tPassed Tests: %s\n", rdsRecoveryPoint, rdsPassed))
                    .append(String.format("S3 Recovery Point: %s\n\tPassed Tests: %s\n", s3RecoveryPoint, s3Passed))
                    .append(String.format("Web App Validation\n\tPassed Tests: %s\n", (systemPassed != null ? systemPassed : "Not Tested")));

            passed = ec2Passed && rdsPassed && s3Passed;

            if (passed)
                passed = systemPassed != null ? systemPassed : false;
        } else {
            stringBuilder.append("Program terminated because of an error.\n").append(thrown);
        }
        if (!passed) {
            Files.lines(file).forEach((line) -> {
                String loggerName = line.split(" - ", 1)[0];
                if (!sortedReportItems.containsKey(loggerName))
                    sortedReportItems.put(loggerName, new LinkedList<>());
                sortedReportItems.get(loggerName).add(line);
            });

            stringBuilder
                    .append("\n")
                    .append("Tests Failed:\n");

            for (Map.Entry<String, List<String>> failedTests : sortedReportItems.entrySet()) {
                failedTests.getValue().forEach(s -> stringBuilder.append(s).append("\n"));
            }
        }

        Path finalReport = Paths.get(Util.APP_DIR_NAME + "Reports", "final-report" + date + ".txt");
        Files.createDirectories(finalReport.getParent());

        try(BufferedWriter bufferedWriter = Files.newBufferedWriter(finalReport)){
            bufferedWriter.write(stringBuilder.toString());
        }

        if(settings.getSnsTopicArn() != null && !settings.getSnsTopicArn().isEmpty())
            Notification.sendSnsMessage(stringBuilder.toString(), settings.getSnsTopicArn(), snsClient);
    }

    /**
     * Report with no exception to include.
     * @throws IOException if there is an IO error
     */
    private void report() throws IOException {
        report(null);
    }


}
