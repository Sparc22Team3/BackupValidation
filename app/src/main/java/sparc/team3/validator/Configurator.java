package sparc.team3.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import sparc.team3.validator.util.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Load program settings from config file.
 */
public class Configurator {
    /**
     * Path of json file with program settings
     */
    private Path configFile;

    /**
     * Creates the default config directory {@link Util#DEFAULT_CONFIG_DIR} (if it doesn't exist) before calling {@link #setConfigFile setConfigFile} to set configFile to the default location.
     * @throws IOException if an I/O error occurs
     */
    public Configurator() throws IOException {
        // Create the config directory for the application
        if(!Files.exists(Util.DEFAULT_CONFIG_DIR)){
            Files.createDirectories(Util.DEFAULT_CONFIG_DIR);
        }
        setConfigFile(Util.DEFAULT_CONFIG_DIR);
    }

    /**
     * Sets up the Path to the location of the config file passed in via command line before calling {@link #setConfigFile setConfigFile} to set configFile.
     * @param configFileLocation the string location of the config file
     */
    public Configurator(String configFileLocation) {
        Path configFile = Paths.get(configFileLocation);
        setConfigFile(configFile);
    }

    /**
     * If path is a directory, configFile is set to config.json in the directory provided.  Otherwise, sets configFile to the file provided.
     * @param path the Path of the configFile or directory where config file is located.
     */
    private void setConfigFile(Path path) {
        if (Files.isDirectory(path)) {
            path = path.resolve(Util.DEFAULT_CONFIG_FILENAME);
        }

        this.configFile = path;
    }

    /**
     * Reads in the settings from the config file and returns a Settings object with the program settings.
     * @return the Settings object with program settings for this instance of the program
     * @throws IOException if an I/O error occurs
     */
    public Settings loadSettings() throws IOException {
        if (configFile == null)
            return null;

        if (!Files.exists(configFile))
            throw new FileNotFoundException(configFile.toString() + " does not exist. Unable to load settings");

        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(configFile.toFile(), Settings.class);
    }

    /**
     * Populates a {@link Settings} object with dummy values, serializes the object to json, and creates the {@link #configFile}.
     * @throws IOException if an I/O error occurs
     */
    public void createBlankConfigFile() throws IOException {
        if(Files.exists(configFile)){
            String filename = "backup." + configFile.getFileName().toString();
            Path backup = configFile.resolveSibling(filename);
            Files.move(configFile, backup);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Create settings map
        HashMap<String, String> map = new HashMap<>();
        map.put("setting", "[ec2|rds]_hostname");
        // Create ConfigFile
        ServerConfigFile cf1 = new ServerConfigFile("ConfigFile1", "PathOnServer", map);
        ServerConfigFile cf2 = new ServerConfigFile("ConfigFile2", "PathOnServer", map);
        // Create list of ConfigFiles
        LinkedList<ServerConfigFile> fileList = new LinkedList<>();
        fileList.add(cf1);
        fileList.add(cf2);
        SecurityGroup sg1 = new SecurityGroup("security group 1 id ", "security group 1 name");
        SecurityGroup sg2 = new SecurityGroup("security group 2 id ", "security group 2 name");
        LinkedList<SecurityGroup> sgList = new LinkedList<>();
        sgList.add(sg1);
        sgList.add(sg2);
        InstanceSettings ec2Settings = new InstanceSettings(sgList, "subnet id", "subnet name");
        InstanceSettings rdsSettings = new InstanceSettings(sgList, "subnet id", "subnet name");
        InstanceSettings s3Settings = new InstanceSettings(sgList, "subnet id", "subnet name");

        // Create Settings object
        Settings settings = new Settings("ec2-user",
                "testKeyFile",
                fileList,
                "us-east-1a",
                "vpc id",
                "vpc name",
                ec2Settings,
                rdsSettings,
                s3Settings);
        mapper.writeValue(configFile.toFile(), settings);
    }

    /**
     * Replaces placeholders for instance hostnames/names in the {@link ServerConfigFile ServerConfigFiles} in Settings with the values passed in.
     * @param ec2 the string of the restored ec2 instance's hostname to replace the ec2 placeholder with
     * @param rds the string of the restored rds instance's hostname to replace the rds placeholder with
     * @param s3 the string of the restored s3 bucket's name to replace the s3 placeholder with
     * @param settings the settings object for the program
     */
    public static void replaceHostname(String ec2, String rds, String s3, Settings settings) {
        String ec2_placeholder = "ec2_hostname";
        String rds_placeholder = "rds_hostname";
        String s3_placeholder = "s3_bucketname";

        for (ServerConfigFile configFile : settings.getConfigFiles()) {
            configFile.getSettings().replaceAll((key, placeholder) -> {
                if (placeholder.equals(ec2_placeholder))
                    return ec2;
                else if (placeholder.equals(rds_placeholder))
                    return rds;
                else if (placeholder.equals(s3_placeholder))
                    return s3;
                return placeholder;
            });
        }
    }

}


