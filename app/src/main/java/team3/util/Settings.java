package team3.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.LinkedList;
@JsonPropertyOrder({"serverUsername", "privateKeyFile"})
public final class Settings {
    private final String serverUsername;
    private final String privateKeyFile;
    private final LinkedList<ServerConfigFile> configFiles;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Settings(@JsonProperty("serverUserName") String serverUsername,
                    @JsonProperty("privateKeyFile") String privateKeyFile,
                    @JsonProperty("configFiles") LinkedList<ServerConfigFile> configFiles) {
        this.serverUsername = serverUsername;
        this.privateKeyFile = privateKeyFile;
        this.configFiles = configFiles;
    }

    public String getServerUsername() {
        return serverUsername;
    }


    public String getPrivateKeyFile() {
        return privateKeyFile;
    }


    public LinkedList<ServerConfigFile> getConfigFiles() {
        return configFiles;
    }


}
