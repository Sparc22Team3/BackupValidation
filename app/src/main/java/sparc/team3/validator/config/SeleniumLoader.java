package sparc.team3.validator.config;

import sparc.team3.validator.util.CLI;

import java.io.IOException;

public class SeleniumLoader extends Selenium{
    public SeleniumLoader(CLI cli, String configFileLocation) {
        super(cli, configFileLocation);
    }
}
