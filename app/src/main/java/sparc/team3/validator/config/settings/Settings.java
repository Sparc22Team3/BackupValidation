package sparc.team3.validator.config.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import sparc.team3.validator.util.CLI;

import java.util.*;

/**
 * Settings for the program to run.
 *
 * @see sparc.team3.validator.config.ConfigEditor
 */
@JsonPropertyOrder({"serverUsername", "privateKeyFile"})
public final class Settings {
    private final String serverUsername;
    private final String privateKeyFile;
    private final String dbUsername;
    private final String dbPassword;
    private final Set<String> databases;
    private final List<ServerConfigFile> configFiles;
    private final String awsRegion;
    private final String vpcID;
    private final String vpcName;
    private final InstanceSettings ec2Settings;
    private final InstanceSettings rdsSettings;
    private final InstanceSettings s3Settings;
    private final String snsTopicArn;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Settings(@JsonProperty("serverUserName") String serverUsername,
                    @JsonProperty("privateKeyFile") String privateKeyFile,
                    @JsonProperty("dbUsername") String dbUsername,
                    @JsonProperty("dbPassword") String dbPassword,
                    @JsonProperty("databases") Set<String> databases,
                    @JsonProperty("configFiles") List<ServerConfigFile> configFiles,
                    @JsonProperty("awsRegion") String awsRegion,
                    @JsonProperty("vpcID") String vpcID,
                    @JsonProperty("vpcName") String vpcName,
                    @JsonProperty("SnsTopicArn") String snsTopicArn,
                    @JsonProperty("ec2Settings") InstanceSettings ec2Settings,
                    @JsonProperty("rdsSettings") InstanceSettings rdsSettings,
                    @JsonProperty("s3Settings") InstanceSettings s3Settings) {

        this.serverUsername = serverUsername;
        this.privateKeyFile = privateKeyFile;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.databases = databases;
        this.configFiles = configFiles;
        this.awsRegion = awsRegion;
        this.vpcID = vpcID;
        this.vpcName = vpcName;
        this.snsTopicArn = snsTopicArn;
        this.ec2Settings = ec2Settings;
        this.rdsSettings = rdsSettings;
        this.s3Settings = s3Settings;
    }

    public String getServerUsername() {
        return serverUsername;
    }


    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public Set<String> getDatabases() {
        return databases;
    }

    public List<ServerConfigFile> getConfigFiles() {

        return configFiles;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public String getVpcID() {
        return vpcID;
    }

    public String getVpcName() {
        return vpcName;
    }

    public String getSnsTopicArn(){
        return snsTopicArn;
    }

    public InstanceSettings getEc2Settings() {
        if (ec2Settings == null)
            return new InstanceSettings(null, null, null, null);
        return ec2Settings;
    }

    public InstanceSettings getRdsSettings() {
        if (ec2Settings == null)
            return new InstanceSettings(null, null, null, null);
        return rdsSettings;
    }

    public InstanceSettings getS3Settings() {
        if (ec2Settings == null)
            return new InstanceSettings(null, null, null, null);
        return s3Settings;
    }

    public SettingsBuilders toBuilders() {
        SettingsBuilder settingsBuilder = SettingsBuilder.builder().serverUsername(serverUsername).privateKeyFile(privateKeyFile)
                .dbUsername(dbUsername).dbPassword(dbPassword).awsRegion(awsRegion).vpcID(vpcID).vpcName(vpcName);

        Map<String, ServerConfigFile.ServerConfigFileBuilder> configFileBuilders = new HashMap<>();
        if (configFiles != null) {
            for (ServerConfigFile configFile : configFiles)
                configFileBuilders.put(configFile.getFullFilePath(), configFile.toBuilder());
        }

        InstanceSettings.InstanceSettingsBuilder ec2SettingsBuilder;
        if (ec2Settings != null)
            ec2SettingsBuilder = ec2Settings.toBuilder();
        else
            ec2SettingsBuilder = InstanceSettings.InstanceSettingsBuilder.builder();

        InstanceSettings.InstanceSettingsBuilder rdsSettingsBuilder;
        if (rdsSettings != null)
            rdsSettingsBuilder = rdsSettings.toBuilder();
        else
            rdsSettingsBuilder = InstanceSettings.InstanceSettingsBuilder.builder();

        InstanceSettings.InstanceSettingsBuilder s3SettingsBuilder;
        if (s3Settings != null)
            s3SettingsBuilder = s3Settings.toBuilder();
        else
            s3SettingsBuilder = InstanceSettings.InstanceSettingsBuilder.builder();

        return new SettingsBuilders(settingsBuilder, ec2SettingsBuilder, rdsSettingsBuilder, s3SettingsBuilder, configFileBuilders);
    }

    @Override
    public String toString() {
        return "Settings{\n" +
                "\tserverUsername='" + serverUsername + "'\n" +
                "\tprivateKeyFile='" + privateKeyFile + "'\n" +
                "\tdbUsername='" + dbUsername + "'\n" +
                "\tdbPassword='" + dbPassword + "'\n" +
                "\tdatabases=" + databases + "\n" +
                "\tawsRegion='" + awsRegion + "'\n" +
                "\tvpcID='" + vpcID + "'\n" +
                "\tvpcName='" + vpcName + "'\n" +
                "\tconfigFiles=" + configFiles + "\n" +
                "\tec2Settings=" + ec2Settings + "\n" +
                "\trdsSettings=" + rdsSettings + "\n" +
                "\ts3Settings=" + s3Settings + "\n" +
                '}';
    }

    public static class SettingsBuilders {
        final SettingsBuilder settingsBuilder;
        final InstanceSettings.InstanceSettingsBuilder ec2SettingsBuilder;
        final InstanceSettings.InstanceSettingsBuilder rdsSettingsBuilder;
        final InstanceSettings.InstanceSettingsBuilder s3SettingsBuilder;
        final Map<String, ServerConfigFile.ServerConfigFileBuilder> configFileBuilders;

        public SettingsBuilders(SettingsBuilder settingsBuilder, InstanceSettings.InstanceSettingsBuilder ec2SettingsBuilder,
                                InstanceSettings.InstanceSettingsBuilder rdsSettingsBuilder, InstanceSettings.InstanceSettingsBuilder s3SettingsBuilder,
                                Map<String, ServerConfigFile.ServerConfigFileBuilder> configFileBuilders) {
            this.settingsBuilder = settingsBuilder;
            this.ec2SettingsBuilder = ec2SettingsBuilder;
            this.rdsSettingsBuilder = rdsSettingsBuilder;
            this.s3SettingsBuilder = s3SettingsBuilder;
            this.configFileBuilders = configFileBuilders;
        }

        public SettingsBuilder getSettingsBuilder() {
            return settingsBuilder;
        }

        public InstanceSettings.InstanceSettingsBuilder getEc2SettingsBuilder() {
            return ec2SettingsBuilder;
        }

        public InstanceSettings.InstanceSettingsBuilder getRdsSettingsBuilder() {
            return rdsSettingsBuilder;
        }

        public InstanceSettings.InstanceSettingsBuilder getS3SettingsBuilder() {
            return s3SettingsBuilder;
        }

        public Map<String, ServerConfigFile.ServerConfigFileBuilder> getConfigFileBuilders() {
            return configFileBuilders;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class SettingsBuilder {
        private String serverUsername;
        private String privateKeyFile;
        private String dbUsername;
        private String dbPassword;
        private Set<String> databases;
        private Set<ServerConfigFile> configFiles;
        private String awsRegion;
        private String vpcID;
        private String vpcName;
        private String snsTopicArn;
        private InstanceSettings ec2Settings;
        private InstanceSettings rdsSettings;
        private InstanceSettings s3Settings;

        private SettingsBuilder() {
        }

        public static SettingsBuilder builder() {
            return new SettingsBuilder();
        }

        public SettingsBuilder serverUsername(String serverUsername) {
            this.serverUsername = serverUsername;
            return this;
        }

        public SettingsBuilder privateKeyFile(String privateKeyFile) {
            this.privateKeyFile = privateKeyFile;
            return this;
        }

        public SettingsBuilder dbUsername(String dbUsername) {
            this.dbUsername = dbUsername;
            return this;
        }

        public SettingsBuilder dbPassword(String dbPassword) {
            this.dbPassword = dbPassword;
            return this;
        }

        public SettingsBuilder database(String database) {
            if (databases == null)
                databases = new HashSet<>();
            databases.add(database);
            return this;
        }

        public SettingsBuilder removeDatabase(String database) {
            if (databases == null) {
                databases = new HashSet<>();
            }
            databases.remove(database);
            return this;
        }

        public SettingsBuilder clearDatabases() {
            if (databases == null) {
                databases = new HashSet<>();
            }
            databases.clear();
            return this;
        }

        public SettingsBuilder awsRegion(String awsRegion) {
            this.awsRegion = awsRegion;
            return this;
        }

        public SettingsBuilder vpcID(String vpcID) {
            this.vpcID = vpcID;
            return this;
        }

        public SettingsBuilder vpcName(String vpcName) {
            this.vpcName = vpcName;
            return this;
        }

        public SettingsBuilder snsTopicArn(String snsTopicArn){
            this.snsTopicArn = snsTopicArn;
            return this;
        }

        public SettingsBuilder configFile(ServerConfigFile file) {
            if (configFiles == null)
                configFiles = new HashSet<>();
            this.configFiles.add(file);
            return this;
        }

        public SettingsBuilder clearConfigFiles() {
            if (configFiles != null)
                configFiles.clear();
            return this;
        }

        public SettingsBuilder ec2InstanceSettings(InstanceSettings settings) {
            this.ec2Settings = settings;
            return this;
        }

        public SettingsBuilder rdsInstanceSettings(InstanceSettings settings) {
            this.rdsSettings = settings;
            return this;
        }

        public SettingsBuilder s3InstanceSettings(InstanceSettings settings) {
            this.s3Settings = settings;
            return this;
        }

        public Settings build() {
            List<ServerConfigFile> configFilesCopy = null;
            if (configFiles != null)
                configFilesCopy = List.copyOf(configFiles);
            Set<String> databasesCopy = null;
            if (databases != null)
                databasesCopy = Set.copyOf(databases);

            return new Settings(serverUsername,
                    privateKeyFile,
                    dbUsername,
                    dbPassword,
                    databasesCopy,
                    configFilesCopy,
                    awsRegion,
                    vpcID,
                    vpcName,
                    snsTopicArn,
                    ec2Settings,
                    rdsSettings,
                    s3Settings
            );
        }

        public String getServerUsername() {
            return serverUsername;
        }

        public String getPrivateKeyFile() {
            return privateKeyFile;
        }

        public String getDbUsername() {
            return dbUsername;
        }

        public String getDbPassword() {
            return dbPassword;
        }

        public Set<String> getDatabases() {
            return databases;
        }

        public Set<ServerConfigFile> getConfigFiles() {
            return configFiles;
        }

        public String getAwsRegion() {
            return awsRegion;
        }

        public String getVpcID() {
            return vpcID;
        }

        public String getVpcName() {
            return vpcName;
        }

        public String getSnsTopicArn(){
            return snsTopicArn;
        }

        public InstanceSettings getEc2Settings() {
            return ec2Settings;
        }

        public InstanceSettings getRdsSettings() {
            return rdsSettings;
        }

        public InstanceSettings getS3Settings() {
            return s3Settings;
        }

        public String toString() {
            return serverConnectionSettingsToString() +
                    CLI.ANSI_BLUE + "Server Config Files:\n" + CLI.ANSI_RESET +
                    configFiles + "\n" +
                    CLI.ANSI_CYAN + "EC2 Settings:\n" + CLI.ANSI_RESET +
                    ec2Settings +
                    CLI.ANSI_BLUE + "RDS Settings:\n" + CLI.ANSI_RESET +
                    rdsSettings +
                    CLI.ANSI_PURPLE + "S3 Settings:\n" + CLI.ANSI_RESET +
                    s3Settings;
        }

        public String serverConnectionSettingsToString() {
            return CLI.ANSI_CYAN + "Server Connection Settings:\n" + CLI.ANSI_RESET +
                    "\tServer username: '" + serverUsername + "'\n" +
                    "\tPrivate key file: '" + privateKeyFile + "'\n" +
                    "\tDB Username: '" + dbUsername + "'\n" +
                    "\tDB Password: '" + dbPassword + "'\n" +
                    "\tDatabases: '" + databases + "'\n" +
                    "\tAWS Region: '" + awsRegion + "'\n" +
                    "\tVPC ID: '" + vpcID + "'\n" +
                    "\tVPC Name: '" + vpcName + "'\n" +
                    "\tSNS Topic ARN: '" + snsTopicArn + "'\n";
        }
    }
}
