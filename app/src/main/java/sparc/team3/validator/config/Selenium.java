package sparc.team3.validator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sparc.team3.validator.util.CLI;
import sparc.team3.validator.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

abstract class Selenium {
    /**
     * Path of json file with program settings
     */
    Path seleniumFile;
    final Logger logger;
    final CLI cli;
    /**
     * Creates the default config directory {@link Util#DEFAULT_CONFIG_DIR} (if it doesn't exist) before calling {@link #setSeleniumFile setSeleniumFile} to set selenium file to the default location.
     * @throws IOException if an I/O error occurs
     */
    Selenium(CLI cli) throws IOException {
        // Create the config directory for the application
        if(!Files.exists(Util.DEFAULT_CONFIG_DIR)){
            Files.createDirectories(Util.DEFAULT_CONFIG_DIR);
        }
        setSeleniumFile(Util.DEFAULT_CONFIG_DIR);
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
        this.cli = cli;
    }

    /**
     * Sets up the Path to the location of the config file passed in via command line before calling {@link #setSeleniumFile setConfigFile} to set configFile.
     * @param seleniumFileLocation the string location of the selenium config file
     */
    Selenium(CLI cli, String seleniumFileLocation){
        Path configFile = Paths.get(seleniumFileLocation);
        setSeleniumFile(configFile);
        this.logger = LoggerFactory.getLogger(this.getClass().getName());
        this.cli = cli;
    }

    /**
     * If path is a directory, configFile is set to config.json in the directory provided.  Otherwise, sets configFile to the file provided.
     * @param path the Path of the configFile or directory where selenium config file is located.
     */
    void setSeleniumFile(Path path) {
        if (Files.isDirectory(path)) {
            path = path.resolve(Util.DEFAULT_SELENIUM_FILENAME);
        }

        this.seleniumFile = path;
    }

    /**
     * Checks whether the selenium config file exists.
     * @return
     */
    boolean seleniumFileExists(){
        return Files.exists(seleniumFile);
    }
}
