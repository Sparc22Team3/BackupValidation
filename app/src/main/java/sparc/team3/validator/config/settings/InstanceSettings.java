package sparc.team3.validator.config.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import sparc.team3.validator.config.ConfigLoader;

import java.util.*;

/**
 * Settings for an AWS resource instance.
 *
 * @see ConfigLoader
 */
public final class InstanceSettings {
    private final String productionName;
    private final String backupVault;
    private final List<SecurityGroup> securityGroups;
    private final String subnetName;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public InstanceSettings(@JsonProperty("productionName") String productionName,
                            @JsonProperty("backupVault") String backupVault,
                            @JsonProperty("securityGroups") List<SecurityGroup> securityGroups,
                            @JsonProperty("subnetName") String subnetName) {
        this.productionName = productionName;
        this.backupVault = backupVault;
        this.securityGroups = securityGroups;
        this.subnetName = subnetName;
    }

    public String getProductionName() {
        return productionName;
    }

    public String getBackupVault() {
        return backupVault;
    }

    public List<SecurityGroup> getSecurityGroups() {
        if (securityGroups == null)
            return new ArrayList<>();
        return securityGroups;
    }

    public String getSubnetName() {
        return subnetName;
    }

    public InstanceSettingsBuilder toBuilder() {
        InstanceSettingsBuilder builder = InstanceSettingsBuilder.builder().productionName(productionName).backupVault(backupVault)
                .subnetName(subnetName);
        if (securityGroups != null) {
            for (SecurityGroup sg : securityGroups) {
                builder.securityGroup(sg);
            }
        }
        return builder;
    }

    public String toString() {
        return "\n\t\tInstanceSettings{\n" +
                "\t\t\tproductionName='" + productionName + "'\n" +
                "\t\t\tbackupVault='" + backupVault + "'\n" +
                "\t\t\tsecurityGroups='" + securityGroups + "'\n" +
                "\t\t\tsubnetName=" + subnetName + "\n" +
                "\t}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstanceSettings that = (InstanceSettings) o;

        if (!Objects.equals(productionName, that.productionName))
            return false;
        if (!Objects.equals(backupVault, that.backupVault)) return false;
        if (!Objects.equals(securityGroups, that.securityGroups))
            return false;
        return Objects.equals(subnetName, that.subnetName);
    }

    @Override
    public int hashCode() {
        int result = productionName != null ? productionName.hashCode() : 0;
        result = 31 * result + (backupVault != null ? backupVault.hashCode() : 0);
        result = 31 * result + (securityGroups != null ? securityGroups.hashCode() : 0);
        result = 31 * result + (subnetName != null ? subnetName.hashCode() : 0);
        return result;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class InstanceSettingsBuilder {
        private String productionName;
        private String backupVault;
        private Set<SecurityGroup> securityGroups;
        private String subnetName;

        private InstanceSettingsBuilder() {
        }

        public static InstanceSettingsBuilder builder() {
            return new InstanceSettingsBuilder();
        }

        public InstanceSettingsBuilder productionName(String productionName) {
            this.productionName = productionName;
            return this;
        }

        public InstanceSettingsBuilder backupVault(String backupVault) {
            this.backupVault = backupVault;
            return this;
        }

        public InstanceSettingsBuilder securityGroup(String id, String name) {
            if (securityGroups == null)
                securityGroups = new HashSet<>();
            securityGroups.add(new SecurityGroup(id, name));
            return this;
        }

        public InstanceSettingsBuilder securityGroup(SecurityGroup securityGroup) {
            if (securityGroups == null)
                securityGroups = new HashSet<>();
            securityGroups.add(securityGroup);
            return this;
        }

        public InstanceSettingsBuilder removeSecurityGroup(String id, String name) {
            if (securityGroups != null)
                securityGroups.remove(new SecurityGroup(id, name));
            return this;
        }

        public InstanceSettingsBuilder clearSecurityGroups() {
            if (securityGroups != null)
                securityGroups.clear();
            return this;
        }

        public InstanceSettingsBuilder subnetName(String name) {
            this.subnetName = name;
            return this;
        }

        public InstanceSettings build() {
            List<SecurityGroup> sgs = null;
            if (securityGroups != null)
                sgs = List.copyOf(securityGroups);
            return new InstanceSettings(productionName, backupVault, sgs, subnetName);
        }

        @Override
        public String toString() {
            return "\t\tproductionName='" + productionName + "'\n" +
                    "\t\tbackupVault='" + backupVault + "'\n" +
                    "\t\tsecurityGroups=" + securityGroups + "'\n" +
                    "\t\tsubnetName='" + subnetName + "'\n";
        }

        public String getProductionName() {
            return productionName;
        }

        public String getBackupVault() {
            return backupVault;
        }

        public Set<SecurityGroup> getSecurityGroups() {
            return securityGroups;
        }

        public String getSubnetName() {
            return subnetName;
        }
    }

}
