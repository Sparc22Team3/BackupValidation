package sparc.team3.validator.config.selenium;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class Login implements Comparable<Login> {
    String username;
    String password;
    String usernameCssSelector;
    String passwordCssSelector;
    String entrypoint;

    public Login() {
    }

    public Login(String username, String password, String usernameCssSelector, String passwordCssSelector, String entrypoint) {
        this.username = username;
        this.password = password;
        this.usernameCssSelector = usernameCssSelector;
        this.passwordCssSelector = passwordCssSelector;
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

    public String getUsernameCssSelector() {
        return this.usernameCssSelector;
    }

    public void setUsernameCssSelector(String usernameCssSelector) {
        this.usernameCssSelector = usernameCssSelector;
    }

    public String getPasswordCssSelector() {
        return this.passwordCssSelector;
    }

    public void setPasswordCssSelector(String passwordCssSelector) {
        this.passwordCssSelector = passwordCssSelector;
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
                    if (usernameCssSelector.compareTo(o.usernameCssSelector) == 0) {
                        return passwordCssSelector.compareTo(o.passwordCssSelector);
                    }
                    return usernameCssSelector.compareTo(o.usernameCssSelector);
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
        if (!Objects.equals(usernameCssSelector, login.usernameCssSelector))
            return false;
        if (!Objects.equals(passwordCssSelector, login.passwordCssSelector))
            return false;
        return Objects.equals(entrypoint, login.entrypoint);
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (usernameCssSelector != null ? usernameCssSelector.hashCode() : 0);
        result = 31 * result + (passwordCssSelector != null ? passwordCssSelector.hashCode() : 0);
        result = 31 * result + (entrypoint != null ? entrypoint.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Login{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", usernameCssSelector='" + usernameCssSelector + '\'' +
                ", passwordCssSelector='" + passwordCssSelector + '\'' +
                ", entrypoint='" + entrypoint + '\'' +
                ", complete=" + isComplete() +
                '}';
    }

    @JsonIgnore
    public boolean isComplete() {
        return username != null && password != null && usernameCssSelector != null &&
                passwordCssSelector != null && entrypoint != null;
    }
}
