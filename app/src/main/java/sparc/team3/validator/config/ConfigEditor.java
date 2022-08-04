package sparc.team3.validator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import sparc.team3.validator.util.CLI;
import sparc.team3.validator.util.InstanceSettings;
import sparc.team3.validator.util.ServerConfigFile;
import sparc.team3.validator.util.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConfigEditor extends Config {
    Settings.SettingsBuilder settingsBuilder;
    InstanceSettings.InstanceSettingsBuilder ec2SettingsBuilder;
    InstanceSettings.InstanceSettingsBuilder rdsSettingsBuilder;
    InstanceSettings.InstanceSettingsBuilder s3SettingsBuilder;
    Map<String, ServerConfigFile.ServerConfigFileBuilder> configFileBuilders;

    public ConfigEditor(CLI cli) throws IOException {
        super(cli);
    }

    public ConfigEditor(CLI cli, String configFileLocation) {
        super(cli, configFileLocation);
    }

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

        cli.outColor("PRINT BUILDERS", CLI.ANSI_RED);
        printBuilders();
        cli.outColor("Build Settings", CLI.ANSI_RED);
        Settings settings = build();
        saveSettings(settings);

    }

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

    boolean editServerConnectionSettings() throws IOException {
        String databases;
        if (settingsBuilder.getDatabaseTables() != null)
            databases = settingsBuilder.getDatabaseTables().toString();
        else
            databases = "None";

        ArrayList<Option> options = new ArrayList<>();
        options.add(new Option("Back", null));
        options.add(new Option("Change Server Username", settingsBuilder.getServerUsername()));
        options.add(new Option("Change Private Key File", settingsBuilder.getPrivateKeyFile()));
        options.add(new Option("Change DB Username", settingsBuilder.getDbUsername()));
        options.add(new Option("Change DB Password", settingsBuilder.getDbPassword()));
        options.add(new Option("Add table to DB Tables", databases));
        options.add(new Option("Remove table from DB Tables", databases));
        options.add(new Option("Remove all tables from DB Tables", databases));
        options.add(new Option("Change AWS Region", settingsBuilder.getAwsRegion()));
        options.add(new Option("Change VPC ID", settingsBuilder.getVpcID()));
        options.add(new Option("Change VPC Name", settingsBuilder.getVpcName()));


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
            case "Add table to DB Tables":
                String table;
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, databases);
                //noinspection StatementWithEmptyBody
                while (!(table = cli.prompt("New Table to Add:")).contains(".")) ;
                settingsBuilder.databaseTable(table);
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getDatabaseTables());
                break;
            case "Remove table from DB Tables":
                cli.outColor("Old Value: %s\n", CLI.ANSI_YELLOW, databases);
                settingsBuilder.removeDatabaseTable(cli.prompt("Table to remove :"));
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getDatabaseTables());
                break;
            case "Remove all tables from DB Tables":
                cli.outColor("Old Value: %s", CLI.ANSI_YELLOW, databases);
                settingsBuilder.clearDatabaseTables();
                cli.outColor("New Value: %s\n", CLI.ANSI_GREEN, settingsBuilder.getDatabaseTables());
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

        }

        //promptAndChangeValue("Server Username ", settingsBuilder.getServerUsername());

        return selection != 0;
    }

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

    void serverConnectionSettings() throws IOException {
        cli.outColor("Setup %s:\n", CLI.ANSI_CYAN, Sections.SERVER);
        settingsBuilder.serverUsername(cli.promptDefault("\tEnter server username: ", "ec2-user"));
        settingsBuilder.privateKeyFile(cli.prompt("\tPath to Private Key File: "));

        settingsBuilder.dbUsername(cli.prompt("\tDB Username: "));
        settingsBuilder.dbPassword(cli.prompt("\tDB Password: "));
        cli.out("\tAdd database tables to check.  These tables will be checked from the last common row back.\n" +
                "\tThe tables should be tables that have primary keys that follow with the time the rows were added.\n" +
                "\tEnter blank line when finished adding settings.\n");
        String table;
        while (!(table = cli.prompt("\t\tDatabase Table: <Database.table_name>")).equals("")) {
            while (!table.contains(".")) {
                cli.outColor("\t\tIncorrect format, table must be entered \"DatabaseName.TableName\"\n", CLI.ANSI_RED);
                table = cli.prompt("\t\tDatabase Table: <Database.table_name>");
            }
            settingsBuilder.databaseTable(table);
        }

        settingsBuilder.awsRegion(cli.promptDefault("\tAWS Region: ", "us-east-1"));
        settingsBuilder.vpcID(cli.prompt("\tVPC ID: "));
        settingsBuilder.vpcName(cli.prompt("\tVPC Name: "));

        cli.out(settingsBuilder.serverConnectionSettingsToString());
    }

    void serverConfigFiles() throws IOException {
        cli.outColor("Setup %s:\n", CLI.ANSI_BLUE, Sections.CONFIG);
        boolean result = cli.promptYesOrNo("\tDo you need to modify configuration files on the restored server for the application to function\n" +
                "\t\twith the new restored server hostname, database hostname, or s3 bucketname?");
        if (result) {
            configFileBuilders = new HashMap<>();
            serverConfigFile();
        }

        while (cli.promptYesOrNo("\tAdd another file?")) {
            serverConfigFile();
        }
        cli.out(CLI.ANSI_BLUE + "Server Config Files:\n" + CLI.ANSI_RESET + configFileBuildersToString());
    }

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

    void ec2Settings() throws IOException {
        cli.outColor("Setup %s:\n", CLI.ANSI_CYAN, Sections.EC2);
        ec2SettingsBuilder = InstanceSettings.InstanceSettingsBuilder.builder();
        ec2SettingsBuilder.productionName(cli.prompt("\tProduction instance id: "));
        ec2SettingsBuilder.backupVault(cli.prompt("\tBackup vault name: "));

        cli.out(CLI.ANSI_CYAN + "EC2 Settings:\n" + CLI.ANSI_RESET + ec2SettingsBuilderToString());
    }

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

    void s3Settings() throws IOException {
        cli.outColor("Setup %s:\n", CLI.ANSI_PURPLE, Sections.S3);
        s3SettingsBuilder = InstanceSettings.InstanceSettingsBuilder.builder();
        s3SettingsBuilder.productionName(cli.prompt("\tProduction bucket name: "));
        s3SettingsBuilder.backupVault(cli.prompt("\tBackup vault name: "));
        cli.out(CLI.ANSI_PURPLE + "S3 Settings:\n" + CLI.ANSI_RESET + s3SettingsBuilderToString());
    }

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

    String configFileBuildersToString() {
        if (configFileBuilders == null)
            return "";
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, ServerConfigFile.ServerConfigFileBuilder> entry : configFileBuilders.entrySet()) {
            stringBuilder.append("\tFile: \"").append(entry.getKey()).append("\"\n").append(entry.getValue().toString());
        }
        return stringBuilder.toString();
    }

    String ec2SettingsBuilderToString() {
        return "\tProduction instance id: '" + ec2SettingsBuilder.getProductionName() + "'\n" +
                "\tBackup vault name: '" + ec2SettingsBuilder.getBackupVault() + "'\n";
    }

    String rdsSettingsBuilderToString() {
        return "\tProduction DB identifier: '" + rdsSettingsBuilder.getProductionName() + "'\n" +
                "\tBackup vault name: '" + rdsSettingsBuilder.getBackupVault() + "'\n" +
                "\tSubnet group name: '" + rdsSettingsBuilder.getSubnetName() + "'\n" +
                "\tSecurity groups: " + rdsSettingsBuilder.getSecurityGroups() + "\n";
    }

    String s3SettingsBuilderToString() {
        return "\tProduction bucket name: '" + ec2SettingsBuilder.getProductionName() + "'\n" +
                "\tBackup vault name: '" + ec2SettingsBuilder.getBackupVault() + "'\n";
    }

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

    void saveSettings(Settings settings) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(configFile.toFile(), settings);
    }

    private String pathKeyBuilder(String filename, String path) {
        if (path.endsWith("/"))
            return path + filename;
        return path + "/" + filename;
    }

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

    private static class Option {
        final String action;
        final String origValue;

        Option(String action, String origValue) {
            this.action = action;
            this.origValue = origValue;
        }
    }
}
