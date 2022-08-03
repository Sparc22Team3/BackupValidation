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
}
