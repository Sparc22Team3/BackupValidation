package sparc.team3.validator.util;

/**
 * Information about an AWS resource instance
 */
public final class InstanceData {
    private final String name;
    private final String instanceID;
    private final String arn;
    private final String hostname;
    private final String localIP;
    private final String publicIP;

    private final String service;

    public InstanceData(String name, String instanceID, String arn, String hostname, String localIP, String publicIP, String service) {
        this.name = name;
        this.instanceID = instanceID;
        this.arn = arn;
        this.hostname = hostname;
        this.localIP = localIP;
        this.publicIP = publicIP;
        this.service = service;
    }

    public String getName() {
        return name;
    }

    public String getInstanceID() {
        return instanceID;
    }

    public String getArn() {
        return arn;
    }

    public String getHostname() {
        return hostname;
    }

    public String getLocalIP() {
        return localIP;
    }

    public String getPublicIP() {
        return publicIP;
    }

    public String getService() {
        return service;
    }
}
