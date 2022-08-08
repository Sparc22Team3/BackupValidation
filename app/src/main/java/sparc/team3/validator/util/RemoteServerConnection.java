package sparc.team3.validator.util;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Facilitates making a connection to a remote server
 */
public class RemoteServerConnection {
    final SSHClient ssh;
    final KeyProvider key;
    final String server;
    final String username;

    SFTPClient sftp;

    /**
     * Creates ssh client, loads host keys, loads public keys, and sets server and remote username.
     * @param server the string of the server hostname or ip address to connect to
     * @param username the string of the username on the remote server
     * @param keyFile the string of the location of the private key on the local machine
     * @throws IOException if an I/O error occurs
     */
    RemoteServerConnection(String server, String username, String keyFile) throws IOException {
        ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        key = ssh.loadKeys(keyFile);
        this.server = server;
        this.username = username;
    }

    /**
     * Connects to and signs in to the remote server.
     * @throws IOException if an I/O error occurs
     */
    void openSSHConnection() throws IOException {
        ssh.connect(server);
        ssh.authPublickey(username, key);
    }

    /**
     * Starts an SFTP session with the remote server.
     * @throws IOException if an I/O error occurs
     */
    void openSFTPConnection() throws IOException {
        this.sftp = ssh.newSFTPClient();
    }

    /**
     * Disconnects the ssh connection to the remote server.
     * @throws IOException if an I/O error occurs
     */
    void closeSSHConnection() throws IOException {
        ssh.disconnect();
    }

    /**
     * Closes the SFTP session with the remote server.
     * @throws IOException if an I/O error occurs
     */
    void closeSFTPConnection() throws IOException {
        sftp.close();
    }

    /**
     * Downloads the file to the specified local directory.
     * @param fullFilePath the string of the full path of the file on the remote server
     * @param dir the string of the directory on the local system to download file to
     * @throws IOException if an I/O error occurs
     */
    void downloadFile(String fullFilePath, String dir) throws IOException {
        sftp.get(fullFilePath, dir);
    }

    /**
     * Uploads the local file to the specified path on the remote server.
     * Providing the final full path of the file on the remote server and the desired permissions will set the permissions on the remote file.
     * @param localFile the path of the local file
     * @param path the string of the path to upload local file to on the remote server
     * @param fullFilePath the full path including filename of the file on the remote server
     * @param perms the int permissions to assign to the file on the remote server
     * @throws IOException if an I/O error occurs
     */
    void uploadFile(Path localFile, String path, String fullFilePath, int perms) throws IOException {
        sftp.put(localFile.toString(), path);
        sftp.chmod(fullFilePath, perms);
    }

    /**
     * Uploads the local file to the specified path on the remote server
     * @param localFile the path of the local file
     * @param path the string of the path to upload local file to on the remote server
     * @throws IOException if an I/O error occurs
     */
    void uploadFile(Path localFile, String path) throws IOException {
        sftp.put(localFile.toString(), path);
    }

    /**
     * Removes the specified file from the local system.
     * @param file path of the file to be deleted
     * @throws IOException if an I/O error occurs
     */
    void removeFile(Path file) throws IOException {
            Files.delete(file);
    }


}
