package team3;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RemoteServerConnection {
    final SSHClient ssh;
    final KeyProvider key;
    final String server;
    final String username;

    SFTPClient sftp;

    RemoteServerConnection(String server, String username, String keyFile) throws IOException {
        ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        key = ssh.loadKeys(keyFile);
        this.server = server;
        this.username = username;
    }

    void openSSHConnection() throws IOException {
        ssh.connect(server);
        ssh.authPublickey(username, key);
    }

    void openSFTPConnection() throws IOException {
        this.sftp = ssh.newSFTPClient();
    }

    void closeSSHConnection() throws IOException {
        ssh.disconnect();
    }

    void closeSFTPConnection() throws IOException {
        sftp.close();
    }

    void downloadFile(String fullFilePath, String dir) throws IOException {
        sftp.get(fullFilePath, dir);
    }

    void uploadFile(Path localFile, String path, String fullFilePath, int perms) throws IOException {
        sftp.put(localFile.toString(), path);
        sftp.chmod(fullFilePath, perms);
    }

    void removeFile(Path file) {
        try {
            Files.delete(file);
        } catch (IOException e){
            System.err.format("%s: Unable to delete file: %s\n", e.getClass().getSimpleName(), e.getMessage());
        }
    }


}
