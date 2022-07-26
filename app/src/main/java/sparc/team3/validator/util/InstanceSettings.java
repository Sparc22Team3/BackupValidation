package sparc.team3.validator.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;

/**
 * Settings for an AWS resource instance.
 *
 * @see sparc.team3.validator.Configurator
 */
public final class InstanceSettings {
    private final String backupVault;
    private final LinkedList<SecurityGroup> securityGroups;
    private final String subnetID;
    private final String subnetName;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public InstanceSettings(@JsonProperty("backupVault") String backupVault,
                            @JsonProperty("securityGroups") LinkedList<SecurityGroup> securityGroups,
                            @JsonProperty("subnetID") String subnetID,
                            @JsonProperty("subnetName") String subnetName) {
        this.backupVault = backupVault;
        this.securityGroups = securityGroups;
        this.subnetID = subnetID;
        this.subnetName = subnetName;
    }

    public String getBackupVault() {
        return backupVault;
    }

    public LinkedList<SecurityGroup> getSecurityGroups() {
        return securityGroups;
    }

    public String getSubnetID() {
        return subnetID;
    }

    public String getSubnetName() {
        return subnetName;
    }

    public String toString() {
        return "\n\t\tInstanceSettings{\n" +
                "\t\t\tsecurityGroups='" + securityGroups + "'\n" +
                "\t\t\tsubnetID='" + subnetID + "'\n" +
                "\t\t\tsubnetName=" + subnetName + "\n" +
                "\t}";
    }

}
