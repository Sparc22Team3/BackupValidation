package team3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import team3.util.ServerConfigFile;
import team3.util.Settings;
import team3.util.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;

public class Configurator {
    private Path configFile;
    public static final String defaultConfigName = "config.json";
    public static final String defaultConfigDir = ".config";

    public Configurator() throws IOException {
        Path configHomeDir = Paths.get(System.getProperty("user.home"), defaultConfigDir, Util.dirName);
        // Create the config directory for the application
        if(!Files.exists(configHomeDir)){
            Files.createDirectories(configHomeDir);
        }
        setConfigFile(configHomeDir);
    }

    public Configurator(String configFileLocation) throws IOException {
        Path configFile = Paths.get(configFileLocation);
        setConfigFile(configFile);
    }

    private void setConfigFile(Path configFile) throws IOException {
        if (Files.isDirectory(configFile)) {
            configFile = configFile.resolve(defaultConfigName);
        }

        this.configFile = configFile;
    }

    public Settings readConfigFile() throws IOException {
        if (configFile == null)
            return null;

        if (!Files.exists(configFile))
            throw new FileNotFoundException(configFile.toString() + " does not exist.");

        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(configFile.toFile(), Settings.class);
    }


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
        // Create Settings object
        Settings settings = new Settings("ec2-user", "testKeyFile", fileList);
        mapper.writeValue(configFile.toFile(), settings);
    }


    public static void replaceHostname(String ec2, String rds, Settings settings) {
        String ec2_placeholder = "ec2_hostname";
        String rds_placeholder = "rds_hostname";

        for (ServerConfigFile configFile : settings.getConfigFiles()) {
            configFile.getSettings().replaceAll((key, hostname) -> {
                if (hostname.equals(ec2_placeholder))
                    return ec2;
                else if (hostname.equals(rds_placeholder)) {
                    return rds;
                }
                return hostname;
            });
        }
    }

}


