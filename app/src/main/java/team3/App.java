package team3;

import org.apache.commons.cli.*;
import team3.util.Settings;
import team3.util.Util;

import java.io.IOException;

public class App {
    final Options options;
    final CommandLineParser parser;
    Configurator configurator;
    Settings settings;
    private App(){
        parser = new DefaultParser();
        options = new Options();
        options();
    }

    private void run(String[] args) {

        try {
            CommandLine line = parser.parse(options, args);
            if(line.hasOption("help")){
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(Util.dirName, options);
                return;
            }
            if(line.hasOption("config"))
                    configurator = new Configurator(line.getOptionValue("config"));
                else
                    configurator = new Configurator();

            if(line.hasOption("newconfig")){
                runNewConfig();
                return;
            }

            settings = configurator.loadSettings();

            // Spin up restored instances to get new hostnames/bucket name

            Configurator.replaceHostname("ec2_test", "rds_test", "s3_test", settings);

            System.out.println(settings);

        } catch (ParseException | IOException e) {
            printExceptionAndExit(e);
        }
    }
    private void runNewConfig(){
        try {
            configurator.createBlankConfigFile();

        } catch (IOException e) {
            printException(e);
        }
    }

    private void options(){
        //options.addOption(new Option());
        options.addOption(new Option("h", "help", false, "print this message"));
        options.addOption(Option.builder("c")
                .longOpt("config")
                .hasArg().argName("file")
                .desc("configuration settings json file or directory containing json file for running "
                        + Util.appName + ". If no config file is specified, program will look in " + Util.defaultConfigDir + " for " + Util.defaultConfigFilename).build()
        );
        options.addOption(new Option("n", "newconfig", false, "create config template file at "
                + "location provided to config or at default location in " + Util.defaultConfigDir + " if config is not specified"));

    }

    public static void printException(Exception e){
        System.err.format("%s: %s\n", e.getClass().getSimpleName(), e.getMessage());
    }

    public static void printExceptionAndExit(Exception e){
        printException(e);
        System.exit(-1);
    }


    public static void main(String[] args) {
        App app = new App();
        app.run(args);
    }
}
