package sparc.team3.validator;

import sparc.team3.validator.util.ServerConfigFile;
import sparc.team3.validator.util.Settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Connects to remote server and changes config files on the server.
 */
public class RemoteServerConfigurator extends RemoteServerConnection{
    /**
     * Sets ups connection to remote server, downloads the files specified in {@link Settings}, alters the files according to the settings,
     * uploads the files back to the server and deletes the downloaded files.
     * @param server The string containing the server hostname or ip address to connect to.
     * @param settings The settings object containing the config files to alter and what alterations to make.
     * @throws IOException if an I/O error occurs
     */
    public RemoteServerConfigurator(String server, Settings settings) throws IOException {
        super(server, settings.getServerUsername(), settings.getPrivateKeyFile());
        String tempDir = System.getProperty("java.io.tmpdir");
        openSSHConnection();
        openSFTPConnection();

        // Loop through each of the config files that need changes
        for (ServerConfigFile configFile : settings.getConfigFiles()) {
            // Download the config file from the remote server and save it to the local temp directory
            downloadFile(configFile.getFullFilePath(), tempDir);

            Path tempFile = Paths.get(tempDir,configFile.getFilename());

            // Modify and upload the temp config file
            alterConfigFile(configFile.getSettings().entrySet(), tempFile);
            uploadFile(tempFile, configFile.getPath(), configFile.getFullFilePath(), 0664);
            // Clean up our mess
            removeFile(tempFile);
        }

        closeSFTPConnection();
        closeSSHConnection();
    }

    /**
     * Searches through a file altering the specified server settings.
     * @param settings set of map entries with setting to change and the new value to set
     * @param tempFile the path to the local copy of the server config file
     * @throws IOException if an I/O error occurs
     */
    private void alterConfigFile(Set<Map.Entry<String,String>> settings, Path tempFile) throws IOException {
        LinkedList<String> lines = new LinkedList<>();
        boolean changed = false;
        try(BufferedReader r = Files.newBufferedReader(tempFile)) {
            Iterator<Map.Entry<String, String>> it;
            Map.Entry<String, String> entry;
            String regex = "(.*)\\s?=\\s?(\\\"?)(.*\\/\\/)?([^\\\";]*)(\\\"?)(;?)";
            Pattern pattern = Pattern.compile(regex);
            String line;
            while ((line = r.readLine()) != null) {

                it = settings.iterator();
                while (it.hasNext()) {
                    entry = it.next();
                    if (line.stripLeading().startsWith(entry.getKey())) {
                        String subst = "$1 = $2$3" + entry.getValue() + "$5$6";
                        Matcher matcher = pattern.matcher(line);
                        line = matcher.replaceAll(subst);
                        changed = true;
                        it.remove();
                    }
                }
                lines.add(line);
            }
        }

        if(changed && !lines.isEmpty() ){
            try(BufferedWriter w = Files.newBufferedWriter(tempFile)){
                ListIterator<String> li = lines.listIterator();

                while(li.hasNext()){
                    w.write(li.next());
                    if(li.hasNext())
                        w.write('\n');
                }
            }

        } else {
            System.err.println("No settings changed in " + tempFile);
        }

    }
    }
