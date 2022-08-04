package sparc.team3.validator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import sparc.team3.validator.util.CLI;
import sparc.team3.validator.util.selenium.SeleniumSettings;

import java.io.FileNotFoundException;
import java.io.IOException;

public class SeleniumLoader extends Selenium{
    public SeleniumLoader(CLI cli, String configFileLocation) {
        super(cli, configFileLocation);
    }

    /**
     * Reads in the settings from the config file and returns a Settings object with the program settings.
     *
     * @return the Settings object with program settings for this instance of the program
     * @throws IOException if an I/O error occurs
     */
    public SeleniumSettings loadSettings() throws IOException {
        if (seleniumFile == null)
            return null;

        if (!seleniumFileExists()) {
            boolean result = cli.promptYesOrNoColor("Selenium file (%s) does not exist.  Would you like to build it?", CLI.ANSI_PURPLE, seleniumFile.toString());
            if (result) {
                SeleniumEditor seleniumEditor = new SeleniumEditor(cli, seleniumFile.toString());
                seleniumEditor.runBuilder();
            }
            if (!seleniumFileExists())
                throw new FileNotFoundException(seleniumFile.toString() + " does not exist. Unable to load settings");
        }
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(seleniumFile.toFile(), SeleniumSettings.class);
    }
}
