package sparc.team3.validator.util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Default values and other useful tidbits.
 */
public class Util {
    /**
     * The name of the app is {@value}.
     */
    public static final String APP_DISPLAY_NAME = "Backup Validator";
    /**
     * The name of the app without spaces, used for directories and such, is {@value}.
     */
    public static final String APP_DIR_NAME = "BackupValidator";
    /**
     * The default config directory in the user's home directory.
     */
    public static final Path DEFAULT_CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".config", Util.APP_DIR_NAME);
    /**
     * The default config file name is {@value}.
     */
    public static final String DEFAULT_CONFIG_FILENAME = "config.json";
}
