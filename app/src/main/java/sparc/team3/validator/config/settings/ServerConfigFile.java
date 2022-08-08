package sparc.team3.validator.config.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import sparc.team3.validator.util.RemoteServerConfigurator;

import java.util.*;

/**
 * Information about config files on remote server and the settings to change in the specified file.
 *
 * @see RemoteServerConfigurator
 */
public final class ServerConfigFile {
    public static final Set<String> placeholders = Set.of("ec2_hostname", "rds_hostname", "s3_bucketname");
    private final String filename;
    private final String path;
    private final Map<String, String> settings;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ServerConfigFile(@JsonProperty("filename") String filename,
                            @JsonProperty("path") String path,
                            @JsonProperty("settings") Map<String, String> settings
    ) {
        this.filename = filename.strip();
        path = path.strip();
        if (path.charAt(path.length() - 1) == '/')
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

    public Map<String, String> getSettings() {
        return settings;
    }

    public ServerConfigFileBuilder toBuilder() {
        ServerConfigFileBuilder builder = ServerConfigFileBuilder.builder().filename(filename).path(path);
        if (settings != null) {
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                builder.setting(entry.getKey(), entry.getValue());
            }
        }

        return builder;
    }

    @Override
    public String toString() {
        return "\t\tServerConfigFile{\n" +
                "\t\t\tfilename='" + filename + "'\n" +
                "\t\t\tpath='" + path + "'\n" +
                "\t\t\tsettings=" + settings + "\n" +
                "\t\t\tfullFilePath='" + getFullFilePath() + "'\n" +
                "\t}";
    }

    @JsonIgnore
    public String getFullFilePath() {
        return path + '/' + filename;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class ServerConfigFileBuilder {
        private String filename;
        private String path;
        private HashMap<String, String> settings;

        public ServerConfigFileBuilder() {
        }

        public static ServerConfigFileBuilder builder() {
            return new ServerConfigFileBuilder();
        }

        public ServerConfigFileBuilder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public ServerConfigFileBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ServerConfigFileBuilder setting(String settingName, String placeholder) {
            if (placeholders.contains(placeholder)) {
                if (settings == null)
                    settings = new HashMap<>();
                settings.put(settingName, placeholder);
            }
            return this;
        }

        public ServerConfigFileBuilder removeSetting(String settingName) {
            if (settings == null)
                settings = new HashMap<>();
            settings.remove(settingName);
            return this;
        }

        public ServerConfigFileBuilder clearSettings() {
            if (settings != null)
                settings.clear();
            return this;
        }

        public ServerConfigFile build() {
            Map<String, String> map = null;
            if (settings != null)
                map = Collections.unmodifiableMap(settings);
            return new ServerConfigFile(filename, path, map);
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

        @Override
        public String toString() {
            return "\t\tfilename='" + filename + "'\n" +
                    "\t\tpath='" + path + "'\n" +
                    "\t\tsettings=\n" + settingsToString() + "\n";
        }

        private String settingsToString() {
            if (settings == null)
                return "\t\t\tNone";
            StringBuilder stringBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                stringBuilder.append("\t\t\t").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
            return stringBuilder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ServerConfigFileBuilder that = (ServerConfigFileBuilder) o;

            if (!Objects.equals(filename, that.filename)) return false;
            return Objects.equals(path, that.path);
        }

        @Override
        public int hashCode() {
            int result = filename != null ? filename.hashCode() : 0;
            result = 31 * result + (path != null ? path.hashCode() : 0);
            return result;
        }
    }
}
