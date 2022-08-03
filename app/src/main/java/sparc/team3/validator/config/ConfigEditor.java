package sparc.team3.validator.config;

import sparc.team3.validator.util.CLI;

import java.io.BufferedReader;
import java.io.IOException;

public class ConfigEditor extends Config{
    public ConfigEditor(CLI cli) throws IOException {
        super(cli);
    }

    public ConfigEditor(CLI cli, String configFileLocation) {
        super(cli, configFileLocation);
    }

    public void runEditor() {

    }
}
