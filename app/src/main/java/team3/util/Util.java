package team3.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Util {
    public static final String appName = "Backup Validation";
    public static final String dirName = "BackupValidation";
    public static final Path defaultConfigDir = Paths.get(System.getProperty("user.home"), ".config", Util.dirName);
    public static final String defaultConfigFilename = "config.json";
}
