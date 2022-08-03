package sparc.team3.validator.config;

import sparc.team3.validator.util.CLI;
import sparc.team3.validator.util.Settings;

import java.io.IOException;


public class ConfigBuilder extends Config {
    Settings.SettingsBuilder sb;

    public ConfigBuilder(CLI cli) throws IOException {
        super(cli);
    }

    public ConfigBuilder(CLI cli, String configFileLocation) {
        super(cli, configFileLocation);
    }

    public void runBuilder() throws IOException {
        sb = Settings.SettingsBuilder.builder();
        serverConnectionSettings();
        databaseSettings();
        serverConfigFiles();
        generalAwsSettings();
        ec2Settings();
        rdsSettings();
        s3Settings();

        cli.out(sb.toString());

    }

    public void serverConnectionSettings() throws IOException {
        cli.outColor("Setup Server Connection Settings:\n", CLI.ANSI_CYAN);
        sb.serverUsername(cli.promptDefault("\tEnter server username: ", "ec2-user"));
        sb.privateKeyFile(cli.prompt("\tPath to Private Key File: "));

        cli.out(sb.serverConnectionSettingsToString());
    }

    public void databaseSettings() throws IOException {
        cli.outColor("Setup Database Settings:\n", CLI.ANSI_GREEN);
        sb.dbUsername(cli.prompt("\tDB Username: "));
        sb.dbPassword(cli.prompt("\tDB Password: "));
        if (cli.promptYesOrNo("\tWould you like to add a database table to check?")) {
            String table = cli.prompt("\t\tDatabase Table: <Database.table_name>");
            while (!table.contains(".")) {
                table = cli.promptColor("\t\tIncorrect format, table must be entered \"DatabaseName.TableName\"", CLI.ANSI_RED);
            }
            sb.databaseTable(table);
            while (cli.promptYesOrNo("\tAdd another table?")) {
                table = cli.prompt("\t\tDatabase Table: <Database.table_name>");
                while (!table.contains(".")) {
                    table = cli.promptColor("Incorrect format, table must be entered \"DatabaseName.TableName\":", CLI.ANSI_RED);
                }
                sb.databaseTable(table);
            }
        }

        cli.out(sb.databaseSettingsToString());

    }

    public void serverConfigFiles() throws IOException {
        cli.outColor("Setup Server Config Files:\n", CLI.ANSI_RED);
        cli.out(sb.getConfigFiles().toString());
    }

    public void generalAwsSettings() throws IOException {
        cli.outColor("Setup General AWS Settings:\n", CLI.ANSI_BLUE);
        sb.awsRegion(cli.promptDefault("AWS Region: ", "us-east-1"));
        sb.vpcID(cli.prompt("VPC ID: "));
        sb.vpcName(cli.prompt("VPC Name: "));

        cli.out(sb.generalAwsSettingsToString());
    }

    public void ec2Settings() throws IOException {
        cli.outColor("Setup EC2 Settings:\n", CLI.ANSI_CYAN);
        cli.out(sb.getEc2Settings().toString());
    }

    public void rdsSettings() throws IOException {
        cli.outColor("Setup RDS Settings:\n", CLI.ANSI_GREEN);
        cli.out(sb.getRdsSettings().toString());
    }

    public void s3Settings() throws IOException {
        cli.outColor("Setup S3 Settings:\n", CLI.ANSI_RED);
        cli.out(sb.getS3Settings().toString());
    }


}
