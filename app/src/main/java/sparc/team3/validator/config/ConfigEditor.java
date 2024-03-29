package sparc.team3.validator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import sparc.team3.validator.util.Notification;
import sparc.team3.validator.util.CLI;
import sparc.team3.validator.config.settings.InstanceSettings;
import sparc.team3.validator.config.settings.ServerConfigFile;
import sparc.team3.validator.config.settings.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * An editor to build and edit program settings via the command line.
 */
public class ConfigEditor extends Config {
    Settings.SettingsBuilder settingsBuilder;
    InstanceSettings.InstanceSettingsBuilder ec2SettingsBuilder;
    InstanceSettings.InstanceSettingsBuilder rdsSettingsBuilder;
    InstanceSettings.InstanceSettingsBuilder s3SettingsBuilder;
    Map<String, ServerConfigFile.ServerConfigFileBuilder> configFileBuilders;

    public ConfigEditor(CLI cli, String configFileLocation) {
        super(cli, configFileLocation);
    }

    /**
     * Called when user wants to build a new settings file.
     * @throws IOException if there is an IO error
     */
    public void runBuilder() throws IOException {
        if (configFileExists())
            if (!cli.promptYesOrNoColor("%s already exists, do you want to overwrite it?", CLI.ANSI_RED, configFile)) {
                if (cli.promptYesOrNoColor("Do you want to modify %s?", CLI.ANSI_RED, configFile)) {
                    runEditor();
                }
                return;
            }
        settingsBuilder = Settings.SettingsBuilder.builder();
        serverConnectionSettings();
        serverConfigFiles();
        ec2Settings();
        rdsSettings();
        s3Settings();

        printBuilders();
        Settings settings = build();
        saveSettings(settings);

    }

    /**
     * Called if the user wants to edit an existing settings file.
     * @throws IOException if there is an IO error
     */
    public void runEditor() throws IOException {
        ConfigLoader loader = new ConfigLoader(cli, configFile.toString());
        Settings settings = loader.loadSettings();
        Settings.SettingsBuilders builders = settings.toBuilders();
        settingsBuilder = builders.getSettingsBuilder();
        ec2SettingsBuilder = builders.getEc2SettingsBuilder();
        rdsSettingsBuilder = builders.getRdsSettingsBuilder();
        s3SettingsBuilder = builders.getS3SettingsBuilder();
        configFileBuilders = builders.getConfigFileBuilders();

        printBuilders();
        //noinspection StatementWithEmptyBody
        while (editSections()) ;
    }

    /**
     * Selects a section to edit
     * @return boolean
     * @throws IOException if there is an IO error
     */
    @SuppressWarnings("StatementWithEmptyBody")
    boolean editSections() throws IOException {
        int max = 0;
        for (Sections section : Sections.values()) {
            if (section.ordinal() > max)
                max = section.ordinal();
            if (section.ordinal() == 0)
                cli.out("%d: %s\n", 0, section.toString());
            else
                cli.out("%d: Edit %s Section\n", section.ordinal(), section.toString());
        }

        int option = -1;
        while (option < 0 || option > max) {
            String response = cli.promptColor("Select Option Number (0 - %d):", CLI.ANSI_GREEN, max);
            try {
                option = Integer.parseInt(response);
            } catch (NumberFormatException e) {
                cli.outColor("Please select an option between 0 and %d", CLI.ANSI_RED, max);
            }
        }

        Sections sec = Sections.values()[option];

        switch (sec) {
            case SAVE:
                Settings settings = build();
                saveSettings(settings);
                return false;
            case SERVER:
                while (editServerConnectionSettings()) ;
                break;
            case CONFIG:
                while (editConfigFiles()) ;
                break;
            case EC2:
                while (editEc2Settings()) ;
                break;
            case RDS:
                while (editRdsSettings()) ;
                break;
            case S3:
                while (editS3Settings()) ;
                break;
        }

        return true;
    }

    /**
     * Prints options and gets the selection from the user
     * @param options Options to print
     * @return int of selection
     * @throws IOException  if there is an IO error
     */
    int printOptionsAndSelect(ArrayList<Option> options) throws IOException {
        int num = options.size();

        for (int i = 0; i < num; i++) {
            if (options.get(i).origValue != null)
                cli.out("%2d: %-45s: [%s]\n", i, options.get(i).action, options.get(i).origValue);
            else
                cli.out("%2d: %s\n", i, options.get(i).action);
        }
        int sel = -1;
        num--;
        while (sel < 0 || sel > num) {
            String selection = cli.promptColor("Please select an action [0-%d]", CLI.ANSI_GREEN, num);
            try {
                sel = Integer.parseInt(selection);
            } catch (NumberFormatException e) {
                cli.outColor("Please select an option between 0 and %d\n", CLI.ANSI_RED, num);
            }
        }
        return sel;
    }

    /**
     * Edit the server connection and AWS settings
     * @return boolean when user has selected to go back
     * @throws IOException if there is an IO error
     */
    boolean editServerConnectionSettings() throws IOException {
        String databases;
        if (settingsBuilder.getDatabases() != null)
            databases = settingsBuilder.getDatabases().toString();
        else
            databases = "None";

        ArrayList<Option> options = new ArrayList<>();
        options.add(new Option("Back", null));
        options.add(new Option("Change Server Username", settingsBuilder.getServerUsername()));
        options.add(new Option("Change Private Key File", settingsBuilder.getPrivateKeyFile()));
        options.add(new Option("Change DB Username", settingsBuilder.getDbUsername()));
        options.add(new Option("Change DB Password", settingsBuilder.getDbPassword()));
        options.add(new Option("Add Database", databases));
        options.add(new Option("Remove Database", databases));
        options.add(new Option("Remove all Databases", databases));
        options.add(new Option("Change AWS Region", settingsBuilder.getAwsRegion()));
        options.add(new Option("Change VPC ID", settingsBuilder.getVpcID()));
        options.add(new Option("Change VPC Name", settingsBuilder.getVpcName()));
        options.add(new Option("Change SNS Topic ARN", settingsBuilder.getSnsTopicArn()));

        int selection = printOptionsAndSelect(options);

        switch (options.get(selection).action) {
            case "Change Server Username":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, settingsBuilder.getServerUsername());
                settingsBuilder.serverUsername(cli.prompt("New Server Username:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getServerUsername());
                break;
            case "Change Private Key File":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, settingsBuilder.getPrivateKeyFile());
                settingsBuilder.privateKeyFile(cli.prompt("New Private key file:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getPrivateKeyFile());
                break;
            case "Change DB Username":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, settingsBuilder.getDbUsername());
                settingsBuilder.dbUsername(cli.prompt("New DB Username:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getDbUsername());
                break;
            case "Change DB Password":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, settingsBuilder.getDbPassword());
                settingsBuilder.dbPassword(cli.prompt("New DB Password:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getDbPassword());
                break;
            case "Add Database":
                String db;
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, databases);
                //noinspection StatementWithEmptyBody
                while (!(db = cli.prompt("New Database to Add:")).contains(".")) ;
                settingsBuilder.database(db);
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getDatabases());
                break;
            case "Remove Database":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, databases);
                settingsBuilder.removeDatabase(cli.prompt("Database to remove :"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getDatabases());
                break;
            case "Remove all Databases":
                cli.outColor("Old Value: %s", CLI.ANSI_YELLOW, databases);
                settingsBuilder.clearDatabases();
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getDatabases());
                break;
            case "Change AWS Region":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, settingsBuilder.getAwsRegion());
                settingsBuilder.awsRegion(cli.prompt("New AWS Region:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getAwsRegion());
                break;
            case "Change VPC ID":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, settingsBuilder.getVpcID());
                settingsBuilder.vpcID(cli.prompt("New VPC ID:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getVpcID());
                break;
            case "Change VPC Name":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, settingsBuilder.getVpcName());
                settingsBuilder.vpcName(cli.prompt("New VPC Name:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getVpcName());
                break;
            case "Change SNS Topic ARN":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, settingsBuilder.getSnsTopicArn());
                settingsBuilder.vpcName(cli.prompt("New SNS Topic ARN:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getSnsTopicArn());
                break;

        }

        //promptAndChangeValue("Server Username ", settingsBuilder.getServerUsername());

        return selection != 0;
    }

    /**
     * Edit a config file in the settings
     * @return boolean when the user is finished adding config files.
     * @throws IOException if there is an IO error
     */
    boolean editConfigFiles() throws IOException {
        cli.out("Current Config Files:\n%s", configFileBuildersToString());

        String selection;
        while (!(selection = cli.promptColor("Enter full path of file to edit, or new to add a file, or nothing to go back: ", CLI.ANSI_GREEN)).equals("")) {
            if (selection.equals("new")) {
                serverConfigFile();
            }
            if (configFileBuilders.get(selection) != null) {
                ServerConfigFile.ServerConfigFileBuilder fileBuilder = configFileBuilders.remove(selection);
                //noinspection StatementWithEmptyBody
                while (editConfigFile(fileBuilder)) ;
                String key = pathKeyBuilder(fileBuilder.getFilename(), fileBuilder.getPath());
                configFileBuilders.put(key, fileBuilder);
            } else
                cli.outColor("%s is not currently configured", CLI.ANSI_RED, selection);
        }

        return false;
    }

    /**
     * Edit a config file
     * @param fileBuilder ServerConfigFile.ServerConfigFileBuilder of config file to edit
     * @return boolean when the user is done editing
     * @throws IOException if there is an IO error
     */
    boolean editConfigFile(ServerConfigFile.ServerConfigFileBuilder fileBuilder) throws IOException {
        String settings;
        if (fileBuilder.getSettings() != null)
            settings = fileBuilder.getSettings().toString();
        else
            settings = "None";
        ArrayList<Option> options = new ArrayList<>();
        options.add(new Option("Back", null));
        options.add(new Option("Change filename", fileBuilder.getFilename()));
        options.add(new Option("Change path", fileBuilder.getPath()));
        options.add(new Option("Add a setting", settings));
        options.add(new Option("Remove a setting", settings));
        options.add(new Option("Remove all settings", settings));

        int selection = printOptionsAndSelect(options);

        switch (options.get(selection).action) {
            case "Change filename":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, fileBuilder.getFilename());
                fileBuilder.filename(cli.prompt("New Filename:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, fileBuilder.getFilename());
                break;
            case "Change path":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, fileBuilder.getPath());
                fileBuilder.path(cli.prompt("New path:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, fileBuilder.getPath());
                break;
            case "Add a setting":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, settings);

                String placeholders = String.join("|", ServerConfigFile.placeholders);
                String setting = cli.prompt("[setting] = [%s]: ", placeholders);

                String[] split = setting.trim().split("\\s?=\\s?");
                while (split.length != 2 || !ServerConfigFile.placeholders.contains(split[1])) {
                    cli.outColor("Entry must be in the following format: \"[setting] = [%s]\"\n", CLI.ANSI_RED, placeholders);
                    setting = cli.prompt("[setting] = [%s]: ", placeholders);
                    split = setting.trim().split("\\s?=\\s?");
                }
                fileBuilder.setting(split[0], split[1]);
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, fileBuilder.getSettings());
                break;
            case "Remove a setting":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, settings);
                fileBuilder.removeSetting(cli.prompt("Enter setting to be removed [setting]:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, fileBuilder.getSettings());
                break;
            case "Remove all settings":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, settings);
                fileBuilder.clearSettings();
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, fileBuilder.getSettings());
                break;
        }

        return selection != 0;
    }

    /**
     * Edit the EC2 Instance settings
     * @return boolean when the user is done.
     * @throws IOException if there is an IO error
     */
    boolean editEc2Settings() throws IOException {
        ArrayList<Option> options = new ArrayList<>();
        options.add(new Option("Back", null));
        options.add(new Option("Change production instance id", ec2SettingsBuilder.getProductionName()));
        options.add(new Option("Change backup vault name", ec2SettingsBuilder.getBackupVault()));

        int selection = printOptionsAndSelect(options);

        switch (options.get(selection).action) {
            case "Change production instance id":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, ec2SettingsBuilder.getProductionName());
                ec2SettingsBuilder.productionName(cli.prompt("New production instance id:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, ec2SettingsBuilder.getProductionName());
                break;
            case "Change backup vault name":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, ec2SettingsBuilder.getBackupVault());
                ec2SettingsBuilder.backupVault(cli.prompt("New EC2 production instance id:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, ec2SettingsBuilder.getBackupVault());
                break;
        }
        return selection != 0;
    }

    /**
     * Edit RDS Instance Settings
     * @return boolean when the user is finished
     * @throws IOException if there is an IO error
     */
    boolean editRdsSettings() throws IOException {
        String securityGroups;
        if (rdsSettingsBuilder.getSecurityGroups() != null)
            securityGroups = rdsSettingsBuilder.getSecurityGroups().toString();
        else
            securityGroups = "None";
        ArrayList<Option> options = new ArrayList<>();
        options.add(new Option("Back", null));
        options.add(new Option("Change production DB identifier", rdsSettingsBuilder.getProductionName()));
        options.add(new Option("Change backup vault name", rdsSettingsBuilder.getBackupVault()));
        options.add(new Option("Change subnet group name", rdsSettingsBuilder.getSubnetName()));
        options.add(new Option("Add a security group", securityGroups));
        options.add(new Option("Remove a security group", securityGroups));
        options.add(new Option("Remove all security groups", securityGroups));

        int selection = printOptionsAndSelect(options);

        switch (options.get(selection).action) {
            case "Change production DB identifier":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, rdsSettingsBuilder.getProductionName());
                rdsSettingsBuilder.productionName(cli.prompt("New production DB identifier:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, rdsSettingsBuilder.getProductionName());
                break;
            case "Change backup vault name":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, rdsSettingsBuilder.getBackupVault());
                rdsSettingsBuilder.backupVault(cli.prompt("New backup vault:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, rdsSettingsBuilder.getBackupVault());
                break;
            case "Change subnet group name":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, rdsSettingsBuilder.getProductionName());
                rdsSettingsBuilder.subnetName(cli.prompt("New production DB identifier:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, rdsSettingsBuilder.getProductionName());
                break;
            case "Add a security group":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, securityGroups);
                rdsSettingsBuilder.securityGroup(cli.prompt("\tSecurity group id:"), cli.prompt("\tSecurity Group Name: "));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, rdsSettingsBuilder.getSecurityGroups());
                break;
            case "Remove a security group":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, securityGroups);
                rdsSettingsBuilder.removeSecurityGroup(cli.prompt("\tSecurity group id:"), cli.prompt("\tSecurity Group Name: "));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, rdsSettingsBuilder.getSecurityGroups());
                break;
            case "Remove all security groups":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, securityGroups);
                rdsSettingsBuilder.clearSecurityGroups();
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, rdsSettingsBuilder.getSecurityGroups());
                break;
        }
        return selection != 0;
    }

    /**
     * Edit S3 Bucket Settings
     * @return boolean when the user is finished.
     * @throws IOException  if there is an IO error
     */
    boolean editS3Settings() throws IOException {
        ArrayList<Option> options = new ArrayList<>();
        options.add(new Option("Back", null));
        options.add(new Option("Change production bucket name", s3SettingsBuilder.getProductionName()));
        options.add(new Option("Change backup vault name", s3SettingsBuilder.getBackupVault()));

        int selection = printOptionsAndSelect(options);

        switch (options.get(selection).action) {
            case "Change production bucket name":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, s3SettingsBuilder.getProductionName());
                s3SettingsBuilder.productionName(cli.prompt("New production bucket name:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, s3SettingsBuilder.getProductionName());
                break;
            case "Change backup vault name":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, s3SettingsBuilder.getBackupVault());
                s3SettingsBuilder.backupVault(cli.prompt("New backup vault:"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, s3SettingsBuilder.getBackupVault());
                break;
        }
        return selection != 0;
    }

    /**
     * Build server connection and AWS settings
     * @throws IOException if there is an IO error
     */
    void serverConnectionSettings() throws IOException {
        cli.outColor("Setup %s:\n", CLI.ANSI_CYAN, Sections.SERVER);
        settingsBuilder.serverUsername(cli.promptDefault("\tEnter server username: ", "ec2-user"));
        settingsBuilder.privateKeyFile(cli.prompt("\tPath to Private Key File: "));

        settingsBuilder.dbUsername(cli.prompt("\tDB Username: "));
        settingsBuilder.dbPassword(cli.prompt("\tDB Password: "));
        cli.out("\tAdd databases to check.\n" +
                "\tEnter blank line when finished adding settings.\n");
        String db;
        while (!(db = cli.prompt("\t\tDatabase:")).equals("")) {
            settingsBuilder.database(db);
        }

        settingsBuilder.awsRegion(cli.promptDefault("\tAWS Region: ", "us-east-1"));
        settingsBuilder.vpcID(cli.prompt("\tVPC ID: "));
        settingsBuilder.vpcName(cli.prompt("\tVPC Name: "));
        if(cli.promptYesOrNo("Do you have a SNS topic setup to accept reports?"))
            settingsBuilder.snsTopicArn(cli.prompt("\tSNS Topic ARN to send reports to:"));
        else
            if(cli.promptYesOrNo("Would you like to create an SNS topic? Will be created in AWS region %s\n" +
                    "(Will only accept messages from the IAM credentials being used to run this program without further configuration in the AWS console.)", settingsBuilder.getAwsRegion())) {
                String topicName = cli.prompt("Topic name to create: ");
                SnsClient snsClient = SnsClient.builder().region(Region.of(settingsBuilder.getAwsRegion())).build();
                String topicArn = Notification.createSNSTopic(topicName, snsClient);
                if(!topicArn.isEmpty())
                    settingsBuilder.snsTopicArn(topicArn);
                else
                    cli.outColor("Unable to create SNS topic", CLI.ANSI_RED);
            }
        cli.out(settingsBuilder.serverConnectionSettingsToString());
    }

    /**
     * Add a server config file while building
     * @throws IOException if there is an IO error
     */
    void serverConfigFiles() throws IOException {
        cli.outColor("Setup %s:\n", CLI.ANSI_BLUE, Sections.CONFIG);
        boolean result = cli.promptYesOrNo("\tDo you need to modify configuration files on the restored server for the application to function\n" +
                "\t\twith the new restored server hostname, database server hostname, or s3 bucketname?");
        if (result) {
            configFileBuilders = new HashMap<>();
            serverConfigFile();
        }

        while (cli.promptYesOrNo("\tAdd another file?")) {
            serverConfigFile();
        }
        cli.out(CLI.ANSI_BLUE + "Server Config Files:\n" + CLI.ANSI_RESET + configFileBuildersToString());
    }

    /**
     * Add a server config file.
     * @throws IOException if there is an IO error
     */
    void serverConfigFile() throws IOException {
        String filename = cli.prompt("\tFilename (Without path):");
        while (filename.contains("/")) {
            cli.outColor("\tFilename should not include path\n", CLI.ANSI_RED);
            filename = cli.prompt("\tFilename (Without path):");
        }
        String path = cli.prompt("\tFull Path on Server:");
        ServerConfigFile.ServerConfigFileBuilder builder = ServerConfigFile.ServerConfigFileBuilder.builder();
        builder.filename(filename).path(path);
        String key = pathKeyBuilder(filename, path);

        configFileBuilders.put(key, builder);
        String setting;
        cli.outColor("\t\tUse the following placeholders \"%s\" to indicate which new value the setting should get.\n" +
                "\t\tEnter blank line when finished adding settings.\n", CLI.ANSI_YELLOW, String.join(", ", ServerConfigFile.placeholders));

        String placeholders = String.join("|", ServerConfigFile.placeholders);
        while (!(setting = cli.prompt("\t\t\t[setting] = [%s]: ", placeholders)).equals("")) {
            String[] split = setting.trim().split("\\s?=\\s?");
            while (split.length != 2 || !ServerConfigFile.placeholders.contains(split[1])) {
                cli.outColor("\t\t\tEntry must be in the following format: \"[setting] = [%s]\"\n", CLI.ANSI_RED, placeholders);
                setting = cli.prompt("\t\t\t[setting] = [%s]: ", placeholders);
                split = setting.trim().split("\\s?=\\s?");
            }

            builder.setting(split[0], split[1]);
        }

    }

    /**
     * Build EC2 Instance settings
     * @throws IOException  if there is an IO error
     */
    void ec2Settings() throws IOException {
        cli.outColor("Setup %s:\n", CLI.ANSI_CYAN, Sections.EC2);
        ec2SettingsBuilder = InstanceSettings.InstanceSettingsBuilder.builder();
        ec2SettingsBuilder.productionName(cli.prompt("\tProduction instance id: "));
        ec2SettingsBuilder.backupVault(cli.prompt("\tBackup vault name: "));

        cli.out(CLI.ANSI_CYAN + "EC2 Settings:\n" + CLI.ANSI_RESET + ec2SettingsBuilderToString());
    }

    /**
     * Build RDS Instance settings
     * @throws IOException if there is an IO error
     */
    void rdsSettings() throws IOException {
        cli.outColor("Setup %s:\n", CLI.ANSI_BLUE, Sections.RDS);
        rdsSettingsBuilder = InstanceSettings.InstanceSettingsBuilder.builder();
        rdsSettingsBuilder.productionName(cli.prompt("\tProduction DB identifier: "));
        rdsSettingsBuilder.backupVault(cli.prompt("\tBackup vault name: "));
        rdsSettingsBuilder.subnetName(cli.prompt("\tSubnet group name: "));
        rdsSettingsBuilder.securityGroup(cli.prompt("\tSecurity group id:"), cli.prompt("\tSecurity Group Name: "));

        while (cli.promptYesOrNo("Do you need to add another security group?")) {
            rdsSettingsBuilder.securityGroup(cli.prompt("\tSecurity group id:"), cli.prompt("\tSecurity Group Name: "));
        }
        cli.out(CLI.ANSI_BLUE + "RDS Settings:\n" + CLI.ANSI_RESET + rdsSettingsBuilderToString());
    }

    /**
     * Build S3 Bucket settings
     * @throws IOException if there is an IO error
     */
    void s3Settings() throws IOException {
        cli.outColor("Setup %s:\n", CLI.ANSI_PURPLE, Sections.S3);
        s3SettingsBuilder = InstanceSettings.InstanceSettingsBuilder.builder();
        s3SettingsBuilder.productionName(cli.prompt("\tProduction bucket name: "));
        s3SettingsBuilder.backupVault(cli.prompt("\tBackup vault name: "));
        cli.out(CLI.ANSI_PURPLE + "S3 Settings:\n" + CLI.ANSI_RESET + s3SettingsBuilderToString());
    }

    /**
     * Print all the builders to print current state of settings before building final Settings object
     */
    void printBuilders() {
        cli.out(CLI.ANSI_YELLOW + "############## Current Settings ##############\n" + CLI.ANSI_RESET +
                settingsBuilder.serverConnectionSettingsToString() +
                CLI.ANSI_BLUE + "Server Config Files:\n" + CLI.ANSI_RESET +
                configFileBuildersToString() +
                CLI.ANSI_CYAN + "EC2 Settings:\n" + CLI.ANSI_RESET +
                ec2SettingsBuilderToString() +
                CLI.ANSI_BLUE + "RDS Settings:\n" + CLI.ANSI_RESET +
                rdsSettingsBuilderToString() +
                CLI.ANSI_PURPLE + "S3 Settings:\n" + CLI.ANSI_RESET +
                s3SettingsBuilderToString() +
                CLI.ANSI_YELLOW + "##############################################\n" + CLI.ANSI_RESET);
    }

    /**
     * Get a string of the ConfigFileBuilders for the editor and builder
     * @return String of the configFile builders
     */
    String configFileBuildersToString() {
        if (configFileBuilders == null)
            return "";
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, ServerConfigFile.ServerConfigFileBuilder> entry : configFileBuilders.entrySet()) {
            stringBuilder.append("\tFile: \"").append(entry.getKey()).append("\"\n").append(entry.getValue().toString());
        }
        return stringBuilder.toString();
    }

    /**
     * Get a string of the EC2SettingsBuilder for the editor and builder
     * @return String of the EC2SettingsBuilder
     */
    String ec2SettingsBuilderToString() {
        return "\tProduction instance id: '" + ec2SettingsBuilder.getProductionName() + "'\n" +
                "\tBackup vault name: '" + ec2SettingsBuilder.getBackupVault() + "'\n";
    }

    /**
     * Get a string of the RDSSettingBuilder for the editor and builder
     * @return String of the RDSSettingsBuilder
     */
    String rdsSettingsBuilderToString() {
        return "\tProduction DB identifier: '" + rdsSettingsBuilder.getProductionName() + "'\n" +
                "\tBackup vault name: '" + rdsSettingsBuilder.getBackupVault() + "'\n" +
                "\tSubnet group name: '" + rdsSettingsBuilder.getSubnetName() + "'\n" +
                "\tSecurity groups: " + rdsSettingsBuilder.getSecurityGroups() + "\n";
    }

    /**
     * Get a string of the S3SettingsBuilder for the editor and builder
     * @return String of the S3SettingsBuilder
     */
    String s3SettingsBuilderToString() {
        return "\tProduction bucket name: '" + ec2SettingsBuilder.getProductionName() + "'\n" +
                "\tBackup vault name: '" + ec2SettingsBuilder.getBackupVault() + "'\n";
    }

    /**
     * Build a full settings object
     * @return Settings object
     */
    Settings build() {
        if (configFileBuilders != null) {
            for (ServerConfigFile.ServerConfigFileBuilder serverConfigFileBuilder : configFileBuilders.values()) {
                settingsBuilder.configFile(serverConfigFileBuilder.build());
            }
        }
        settingsBuilder.ec2InstanceSettings(ec2SettingsBuilder.build())
                .rdsInstanceSettings(rdsSettingsBuilder.build())
                .s3InstanceSettings(s3SettingsBuilder.build());
        return settingsBuilder.build();
    }

    /**
     * Save the settings to file
     * @param settings Settings object to save
     * @throws IOException if there is an IO error
     */
    void saveSettings(Settings settings) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(configFile.toFile(), settings);
    }

    /**
     * Build a path key from the filename and Path
     * @param filename String of filename
     * @param path Path of path to directory
     * @return String of path/to/filename
     */
    private String pathKeyBuilder(String filename, String path) {
        if (path.endsWith("/"))
            return path + filename;
        return path + "/" + filename;
    }

    /**
     * Different sections of settings
     */
    private enum Sections {
        SAVE("Save Settings"),
        SERVER("Server Connection Settings"),
        CONFIG("Server Config Files"),
        EC2("EC2 Settings"),
        RDS("RDS Settings"),
        S3("S3 Settings");


        private final String section;

        Sections(String s) {
            section = s;
        }

        @Override
        public String toString() {
            return section;
        }
    }

    /**
     * Options to build menus
     */
    private static class Option {
        final String action;
        final String origValue;

        Option(String action, String origValue) {
            this.action = action;
            this.origValue = origValue;
        }
    }
}
