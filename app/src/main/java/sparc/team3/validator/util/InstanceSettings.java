package sparc.team3.validator.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import sparc.team3.validator.config.ConfigLoader;

import java.util.LinkedList;

/**
 * Settings for an AWS resource instance.
 *
 * @see ConfigLoader
 */
public final class InstanceSettings {
    private final String productionName;
    private final String backupVault;
    private final LinkedList<SecurityGroup> securityGroups;
    private final String subnetName;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public InstanceSettings(@JsonProperty("productionName") String productionName,
                            @JsonProperty("backupVault") String backupVault,
                            @JsonProperty("securityGroups") LinkedList<SecurityGroup> securityGroups,
                            @JsonProperty("subnetName") String subnetName) {
        this.productionName = productionName;
        this.backupVault = backupVault;
        this.securityGroups = securityGroups;
        this.subnetName = subnetName;
    }
    public String getProductionName(){
        return productionName;
    }

    public String getBackupVault() {
        return backupVault;
    }

    public LinkedList<SecurityGroup> getSecurityGroups() {
        return securityGroups;
    }

    public String getSubnetName() {
        return subnetName;
    }

    public String toString() {
        return "\n\t\tInstanceSettings{\n" +
                "\t\t\tproductionName='" + productionName + "'\n" +
                "\t\t\tbackupVault='" + backupVault + "'\n" +
                "\t\t\tsecurityGroups='" + securityGroups + "'\n" +
                "\t\t\tsubnetName=" + subnetName + "\n" +
                "\t}";
    }

}
