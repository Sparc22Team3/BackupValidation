package team3.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;

public class InstanceSettings {
    private final LinkedList<SecurityGroup> securityGroups;
    private final String subnetID;
    private final String subnetName;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public InstanceSettings(@JsonProperty("securityGroups") LinkedList<SecurityGroup> securityGroups,
                            @JsonProperty("subnetID") String subnetID,
                            @JsonProperty("subnetName") String subnetName) {
        this.securityGroups = securityGroups;
        this.subnetID = subnetID;
        this.subnetName = subnetName;
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

}
