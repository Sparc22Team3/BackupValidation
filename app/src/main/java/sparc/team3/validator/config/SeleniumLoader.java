package sparc.team3.validator.config;

import sparc.team3.validator.util.CLI;

import java.io.IOException;

public class SeleniumLoader extends Selenium{
    SeleniumLoader(CLI cli) throws IOException {
        super(cli);
    }

    SeleniumLoader(CLI cli, String configFileLocation) {
        super(cli, configFileLocation);
    }
}
