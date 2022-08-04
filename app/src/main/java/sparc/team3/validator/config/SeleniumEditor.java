package sparc.team3.validator.config;

import sparc.team3.validator.util.CLI;

import java.io.IOException;

public class SeleniumEditor extends Selenium{
    SeleniumEditor(CLI cli) throws IOException {
        super(cli);
    }

    SeleniumEditor(CLI cli, String configFileLocation) {
        super(cli, configFileLocation);
    }
}
