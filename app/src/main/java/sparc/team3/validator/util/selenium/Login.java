package sparc.team3.validator.util.selenium;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class Login implements Comparable<Login> {
    String username;
    String password;
    String usernameFieldID;
    String passwordFieldID;
    String entrypoint;

    public Login() {
    }

    public Login(String username, String password, String usernameFieldID, String passwordFieldID, String entrypoint) {
        this.username = username;
        this.password = password;
        this.usernameFieldID = usernameFieldID;
        this.passwordFieldID = passwordFieldID;
        this.entrypoint = entrypoint;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsernameFieldID() {
        return this.usernameFieldID;
    }

    public void setUsernameFieldID(String usernameFieldID) {
        this.usernameFieldID = usernameFieldID;
    }

    public String getPasswordFieldID() {
        return this.passwordFieldID;
    }

    public void setPasswordFieldID(String passwordFieldID) {
        this.passwordFieldID = passwordFieldID;
    }

    public String getEntrypoint() {
        return this.entrypoint;
    }

    public void setEntrypoint(String entrypoint) {
        this.entrypoint = entrypoint;
    }

    @Override
    public int compareTo(Login o) {
        if (username.compareTo(o.username) == 0) {
            if (password.compareTo(o.password) == 0) {
                if (entrypoint.compareTo(o.entrypoint) == 0) {
                    if (usernameFieldID.compareTo(o.usernameFieldID) == 0) {
                        return passwordFieldID.compareTo(o.passwordFieldID);
                    }
                    return usernameFieldID.compareTo(o.usernameFieldID);
                }
                return entrypoint.compareTo(o.entrypoint);
            }
            return password.compareTo(o.password);
        }
        return username.compareTo(o.password);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Login login = (Login) o;

        if (!Objects.equals(username, login.username)) return false;
        if (!Objects.equals(password, login.password)) return false;
        if (!Objects.equals(usernameFieldID, login.usernameFieldID))
            return false;
        if (!Objects.equals(passwordFieldID, login.passwordFieldID))
            return false;
        return Objects.equals(entrypoint, login.entrypoint);
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (usernameFieldID != null ? usernameFieldID.hashCode() : 0);
        result = 31 * result + (passwordFieldID != null ? passwordFieldID.hashCode() : 0);
        result = 31 * result + (entrypoint != null ? entrypoint.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Login{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", usernameFieldID='" + usernameFieldID + '\'' +
                ", passwordFieldID='" + passwordFieldID + '\'' +
                ", entrypoint='" + entrypoint + '\'' +
                ", complete=" + isComplete() +
                '}';
    }

    @JsonIgnore
    public boolean isComplete() {
        return username != null && password != null && usernameFieldID != null &&
                passwordFieldID != null && entrypoint != null;
    }
}
