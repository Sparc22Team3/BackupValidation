package sparc.team3.validator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sparc.team3.validator.util.CLI;
import sparc.team3.validator.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

abstract class Config {
    /**
     * Path of json file with program settings
     */
    Path configFile;
    final Logger logger;
    final CLI cli;

    /**
     * Sets up the Path to the location of the config file passed in via command line before calling {@link #setConfigFile setConfigFile} to set configFile.
     * @param configFileLocation the string location of the config file
     */
    Config(CLI cli, String configFileLocation){
        Path configFile = Paths.get(configFileLocation);
        setConfigFile(configFile);
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
        this.cli = cli;
    }

    /**
     * If path is a directory, configFile is set to config.json in the directory provided.  Otherwise, sets configFile to the file provided.
     * @param path the Path of the configFile or directory where config file is located.
     */
    void setConfigFile(Path path) {
        if (Files.isDirectory(path)) {
            path = path.resolve(Util.DEFAULT_CONFIG_FILENAME);
        }

        this.configFile = path;
    }

    /**
     * Checks whether the config file exists.
     * @return
     */
    boolean configFileExists(){
        return Files.exists(configFile);
    }

    public Path getActualConfigDir(){
        return configFile.getParent();
    }
}
