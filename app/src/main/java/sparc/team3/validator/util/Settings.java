package sparc.team3.validator.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import sparc.team3.validator.config.ConfigLoader;

import java.util.LinkedList;

/**
 * Settings for the program to run.
 *
 * @see ConfigLoader
 */
@JsonPropertyOrder({"serverUsername", "privateKeyFile"})
public final class Settings {
    private final String serverUsername;
    private final String privateKeyFile;
    private final String dbUsername;
    private final String dbPassword;
    private final LinkedList<ServerConfigFile> configFiles;
    private final String awsRegion;
    private final String vpcID;
    private final String vpcName;
    private final InstanceSettings ec2Settings;
    private final InstanceSettings rdsSettings;
    private final InstanceSettings s3Settings;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Settings(@JsonProperty("serverUserName") String serverUsername,
                    @JsonProperty("privateKeyFile") String privateKeyFile,
                    @JsonProperty("dbUsername") String dbUsername,
                    @JsonProperty("dbPassword") String dbPassword, @JsonProperty("configFiles") LinkedList<ServerConfigFile> configFiles,
                    @JsonProperty("awsRegion") String awsRegion,
                    @JsonProperty("vpcID") String vpcID,
                    @JsonProperty("vpcName") String vpcName,
                    @JsonProperty("ec2Settings") InstanceSettings ec2Settings,
                    @JsonProperty("rdsSettings") InstanceSettings rdsSettings,
                    @JsonProperty("s3Settings") InstanceSettings s3Settings) {

        this.serverUsername = serverUsername;
        this.privateKeyFile = privateKeyFile;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.configFiles = configFiles;
        this.awsRegion = awsRegion;
        this.vpcID = vpcID;
        this.vpcName = vpcName;
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

    public LinkedList<ServerConfigFile> getConfigFiles() {
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

    public InstanceSettings getEc2Settings() {
        return ec2Settings;
    }

    public InstanceSettings getRdsSettings() {
        return rdsSettings;
    }

    public InstanceSettings getS3Settings() {
        return s3Settings;
    }

    @Override
    public String toString() {
        return "Settings{\n" +
                "\tserverUsername='" + serverUsername + "'\n" +
                "\tprivateKeyFile='" + privateKeyFile + "'\n" +
                "\tdbUsername='" + dbUsername + "'\n" +
                "\tdbPassword='" + dbPassword + "'\n" +
                "\tawsRegion='" + awsRegion + "'\n" +
                "\tvpcID='" + vpcID + "'\n" +
                "\tvpcName='" + vpcName + "'\n" +
                "\tconfigFiles=" + configFiles + "\n" +
                "\tec2Settings=" + ec2Settings + "\n" +
                "\trdsSettings=" + rdsSettings + "\n" +
                "\ts3Settings=" + s3Settings + "\n" +
                '}';
    }

    public static class SettingsBuilder{
        private String serverUsername;
        private String privateKeyFile;
        private String dbUsername;
        private String dbPassword;
        private LinkedList<String> databaseTables;
        private LinkedList<ServerConfigFile> configFiles;
        private String awsRegion;
        private String vpcID;
        private String vpcName;
        private InstanceSettings ec2Settings;
        private InstanceSettings rdsSettings;
        private InstanceSettings s3Settings;

        private SettingsBuilder() {
        }

        public SettingsBuilder serverUsername(String serverUsername){
            this.serverUsername = serverUsername;
            return this;
        }

        public SettingsBuilder privateKeyFile(String privateKeyFile){
            this.privateKeyFile = privateKeyFile;
            return this;
        }

        public SettingsBuilder dbUsername(String dbUsername){
            this.dbUsername = dbUsername;
            return this;
        }

        public SettingsBuilder dbPassword(String dbPassword){
            this.dbPassword = dbPassword;
            return this;
        }

        public SettingsBuilder databaseTable(String table){
            if(databaseTables == null)
                databaseTables = new LinkedList<>();
            databaseTables.add(table);
            return this;
        }

        public SettingsBuilder awsRegion(String awsRegion){
            this.awsRegion = awsRegion;
            return this;
        }

        public SettingsBuilder vpcID(String vpcID){
            this.vpcID = vpcID;
            return this;
        }

        public SettingsBuilder vpcName(String vpcName){
            this.vpcName = vpcName;
            return this;
        }

        public SettingsBuilder configFile(ServerConfigFile file){
            if(configFiles == null)
                configFiles = new LinkedList<>();
            this.configFiles.add(file);
            return this;
        }

        public SettingsBuilder clearConfigFiles(){
            if(configFiles != null)
                configFiles.clear();
            return this;
        }

        public SettingsBuilder ec2InstanceSettings(InstanceSettings settings){
            this.ec2Settings = settings;
            return this;
        }

        public SettingsBuilder rdsInstanceSettings(InstanceSettings settings){
            this.rdsSettings = settings;
            return this;
        }

        public SettingsBuilder s3InstanceSettings(InstanceSettings settings){
            this.s3Settings = settings;
            return this;
        }


        public Settings build(){
            return new Settings(serverUsername,
                                privateKeyFile,
                                dbUsername,
                                dbPassword,
                                configFiles,
                                awsRegion,
                                vpcID,
                                vpcName,
                                ec2Settings,
                                rdsSettings,
                                s3Settings
                    );
        }


        public static SettingsBuilder builder() {
            return new SettingsBuilder();
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

        public LinkedList<String> getDatabaseTables(){
            return databaseTables;
        }

        public LinkedList<ServerConfigFile> getConfigFiles() {
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

        public InstanceSettings getEc2Settings() {
            return ec2Settings;
        }

        public InstanceSettings getRdsSettings() {
            return rdsSettings;
        }

        public InstanceSettings getS3Settings() {
            return s3Settings;
        }

        public String serverConnectionSettingsToString(){
            return CLI.ANSI_CYAN + "Server Connection Settings:\n" + CLI.ANSI_RESET +
                    "\tServer username:" + serverUsername + "\n" +
                    "\tPrivate key file:" + privateKeyFile + "\n";
        }

        public String databaseSettingsToString(){
            return CLI.ANSI_GREEN + "Database Settings:\n"  + CLI.ANSI_RESET+
                    "\tDB Username:" + dbUsername + "\n" +
                    "\tDB Password:" + dbPassword + "\n" +
                    "\tDB Tables:" + databaseTables + "\n";
        }

        public String generalAwsSettingsToString(){
            return CLI.ANSI_BLUE + "General AWS Settings:\n" + CLI.ANSI_RESET +
                    "\tAWS Region: " + awsRegion + "\n" +
                    "\tVPC ID: " + vpcID + "\n" +
                    "\tVPC Name: " + vpcName + "\n";
        }

        public String toString(){
            return  serverConnectionSettingsToString() + databaseSettingsToString() +
                    CLI.ANSI_RED + "Server Config Files:\n"  + CLI.ANSI_RESET +
                    configFiles + "\n" +
                    generalAwsSettingsToString() +
                    CLI.ANSI_CYAN + "\tEC2 Settings:\n"  + CLI.ANSI_RESET +
                    ec2Settings +
                    CLI.ANSI_GREEN + "\tRDS Settings:\n"  + CLI.ANSI_RESET +
                    rdsSettings +
                    CLI.ANSI_RED + "\tS3 Settings:\n"  + CLI.ANSI_RESET +
                    s3Settings;
        }
    }
}
