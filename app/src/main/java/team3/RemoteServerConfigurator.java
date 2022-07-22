package team3;

import team3.util.ServerConfigFile;
import team3.util.Settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class RemoteServerConfigurator extends RemoteServerConnection{

    public RemoteServerConfigurator(String server, Settings settings) throws IOException {
        super(server, settings.getServerUsername(), settings.getPrivateKeyFile());
        String tempDir = System.getProperty("java.io.tmpdir");
        openSSHConnection();
        openSFTPConnection();

        // Loop through each of the config files that need changes
        for (ServerConfigFile configFile : settings.getConfigFiles()) {
            // Download the config file from the remote server and save it to the local temp directory
            downloadFile(configFile.getFullFilePath(), tempDir);

            Path tempFile = Paths.get(tempDir + File.separatorChar + configFile.getFilename());

            // Modify and upload the temp config file
            alterConfigFile(configFile.getSettings().entrySet(), tempFile);
            uploadFile(tempFile, configFile.getPath(), configFile.getFullFilePath(), 0664);
            // Clean up our mess
            removeFile(tempFile);
        }

        closeSFTPConnection();
        closeSSHConnection();
    }



    private void alterConfigFile(Set<Map.Entry<String,String>> settings, Path tempFile){
        LinkedList<String> lines = new LinkedList<>();
        boolean changed = false;
        try(BufferedReader r = Files.newBufferedReader(tempFile)) {
            Iterator<Map.Entry<String,String>> it;
            Map.Entry<String,String> entry;

            String line;
            while ((line = r.readLine()) != null) {

                it = settings.iterator();
                while(it.hasNext()) {
                    entry = it.next();
                    if(line.stripLeading().startsWith(entry.getKey())){
                        String[] parts = line.split("\\s*=\\s*");
                        line = parts[0] + " = " + entry.getValue();
                        changed = true;
                        it.remove();
                    }
                }
                lines.add(line);
            }

        } catch (IOException e) {
            System.err.format("%s: Unable to read file: %s\n", e.getClass().getSimpleName(), e.getMessage());
        }

        if(changed && !lines.isEmpty() ){
            try(BufferedWriter w = Files.newBufferedWriter(tempFile)){
                ListIterator<String> li = lines.listIterator();

                while(li.hasNext()){
                    w.write(li.next());
                    if(li.hasNext())
                        w.write('\n');
                }
            } catch (IOException e) {
                System.err.format("%s: Unable to write file: %s\n", e.getClass().getSimpleName(), e.getMessage());
            }

        } else {
            System.err.println("No settings changed in " + tempFile);
        }

    }
    }
