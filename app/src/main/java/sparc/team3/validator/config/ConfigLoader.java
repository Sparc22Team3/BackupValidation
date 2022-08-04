package sparc.team3.validator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import sparc.team3.validator.util.CLI;
import sparc.team3.validator.util.ServerConfigFile;
import sparc.team3.validator.util.Settings;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Load program settings from config file.
 */
public class ConfigLoader extends Config {

    public ConfigLoader(CLI cli, String configFileLocation) {
        super(cli, configFileLocation);
    }

    /**
     * Replaces placeholders for instance hostnames/names in the {@link ServerConfigFile ServerConfigFiles} in Settings with the values passed in.
     *
     * @param ec2      the string of the restored ec2 instance's hostname to replace the ec2 placeholder with
     * @param rds      the string of the restored rds instance's hostname to replace the rds placeholder with
     * @param s3       the string of the restored s3 bucket's name to replace the s3 placeholder with
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

    /**
     * Reads in the settings from the config file and returns a Settings object with the program settings.
     *
     * @return the Settings object with program settings for this instance of the program
     * @throws IOException if an I/O error occurs
     */
    public Settings loadSettings() throws IOException {
        if (configFile == null)
            return null;

        if (!configFileExists()) {
            boolean result = cli.promptYesOrNoColor("Config file (%s) does not exist.  Would you like to build it?", CLI.ANSI_PURPLE, configFile.toString());
            if (result) {
                ConfigEditor configEditor = new ConfigEditor(cli, configFile.toString());
                configEditor.runBuilder();
            }
            if (!configFileExists())
                throw new FileNotFoundException(configFile.toString() + " does not exist. Unable to load settings");
        }
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(configFile.toFile(), Settings.class);
    }

}


