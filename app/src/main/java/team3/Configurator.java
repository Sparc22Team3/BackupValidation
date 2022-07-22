package team3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import team3.util.ServerConfigFile;
import team3.util.Settings;
import team3.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class Configurator {
    private File configFile;
    public static final String defaultConfigName = "config.json";
    public static final String defaultConfigDir = ".config";

    public Configurator() throws IOException {
        File configHomeDir = new File (System.getProperty("user.home") + File.separatorChar + defaultConfigDir + File.separatorChar + Util.dirName);
        // Create the config directory for the application
        if(!configHomeDir.exists()){
            if (!configHomeDir.mkdirs())
                throw new IOException("Unable to create directories:" + configHomeDir);
        }
        setConfigFile(configHomeDir);
    }

    public Configurator(String configFileLocation) throws IOException {
        File configFile = new File(configFileLocation);
        setConfigFile(configFile);
    }

    private void setConfigFile(File configFile) throws IOException {
        try {
            if (configFile.isDirectory()) {
                configFile = new File(configFile.getCanonicalPath() + File.separatorChar + defaultConfigName);
            }

            if (!configFile.exists())
                throw new FileNotFoundException(configFile.getCanonicalPath() + " does not exist.  Empty config file created in "
                        + configFile.getParent() + ".");
            this.configFile = configFile;
        } catch (IOException e) {
            createBlankConfigFile(configFile);
            System.out.print(e.getMessage());
            System.exit(2);
        }
    }

    public Settings readConfigFile() throws IOException {
        if (configFile == null)
            return null;
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(configFile, Settings.class);
    }


    public void createBlankConfigFile(File configFile) throws IOException {
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
        mapper.writeValue(configFile, settings);
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


