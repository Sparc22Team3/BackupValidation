package sparc.team3.validator.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;

/**
 * Information about config files on remote server and the settings to change in the specified file.
 *
 * @see sparc.team3.validator.RemoteServerConfigurator
 */
public final class ServerConfigFile {
    private final String filename;
    private final String path;
    private final HashMap<String, String> settings;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ServerConfigFile(@JsonProperty("filename") String filename,
                            @JsonProperty("path") String path,
                            @JsonProperty("settings") HashMap<String, String> settings
                            ) {
        this.filename = filename.strip();
        path = path.strip();
        if(path.charAt(path.length() - 1) == '/')
            path = path.substring(0, path.length() - 2);
        this.path = path;
        this.settings = settings;

    }

    public String getFilename() {
        return filename;
    }


    public String getPath() {
        return path;
    }

    public HashMap<String, String> getSettings() {
        return settings;
    }

    public String getFullFilePath() {
        return path + '/' + filename;
    }

    @Override
    public String toString() {
        return "\n\t\tServerConfigFile{\n" +
                "\t\t\tfilename='" + filename + "'\n" +
                "\t\t\tpath='" + path + "'\n" +
                "\t\t\tsettings=" + settings + "\n" +
                "\t\t\tfullFilePath='" + getFullFilePath() + "'\n" +
                "\t}";
    }
}
