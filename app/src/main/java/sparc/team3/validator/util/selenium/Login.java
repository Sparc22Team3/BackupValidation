package sparc.team3.validator.util.selenium;

public class Login{
    String username;
    String password;
    String usernameFieldID;
    String passwordFieldID;
    String entrypoint;

    public Login(String username, String password, String usernameFieldID, String passwordFieldID, String entrypoint) {
        this.username = username;
        this.password = password;
        this.usernameFieldID = usernameFieldID;
        this.passwordFieldID = passwordFieldID;
        this.entrypoint = entrypoint;
    }

    public static LoginBuilder builder() {
        return new LoginBuilder();
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getUsernameFieldID() {
        return this.usernameFieldID;
    }

    public String getPasswordFieldID() {
        return this.passwordFieldID;
    }

    public String getEntrypoint() {
        return this.entrypoint;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsernameFieldID(String usernameFieldID) {
        this.usernameFieldID = usernameFieldID;
    }

    public void setPasswordFieldID(String passwordFieldID) {
        this.passwordFieldID = passwordFieldID;
    }

    public void setEntrypoint(String entrypoint) {
        this.entrypoint = entrypoint;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Login)) return false;
        final Login other = (Login) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$username = this.getUsername();
        final Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final Object this$password = this.getPassword();
        final Object other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
        final Object this$usernameFieldID = this.getUsernameFieldID();
        final Object other$usernameFieldID = other.getUsernameFieldID();
        if (this$usernameFieldID == null ? other$usernameFieldID != null : !this$usernameFieldID.equals(other$usernameFieldID))
            return false;
        final Object this$passwordFieldID = this.getPasswordFieldID();
        final Object other$passwordFieldID = other.getPasswordFieldID();
        if (this$passwordFieldID == null ? other$passwordFieldID != null : !this$passwordFieldID.equals(other$passwordFieldID))
            return false;
        final Object this$entrypoint = this.getEntrypoint();
        final Object other$entrypoint = other.getEntrypoint();
        if (this$entrypoint == null ? other$entrypoint != null : !this$entrypoint.equals(other$entrypoint))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Login;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        final Object $usernameFieldID = this.getUsernameFieldID();
        result = result * PRIME + ($usernameFieldID == null ? 43 : $usernameFieldID.hashCode());
        final Object $passwordFieldID = this.getPasswordFieldID();
        result = result * PRIME + ($passwordFieldID == null ? 43 : $passwordFieldID.hashCode());
        final Object $entrypoint = this.getEntrypoint();
        result = result * PRIME + ($entrypoint == null ? 43 : $entrypoint.hashCode());
        return result;
    }

    public String toString() {
        return "Login(username=" + this.getUsername() + ", password=" + this.getPassword() + ", usernameFieldID=" + this.getUsernameFieldID() + ", passwordFieldID=" + this.getPasswordFieldID() + ", entrypoint=" + this.getEntrypoint() + ")";
    }

    public static class LoginBuilder {
        private String username;
        private String password;
        private String usernameFieldID;
        private String passwordFieldID;
        private String entrypoint;

        LoginBuilder() {
        }

        public LoginBuilder username(String username) {
            this.username = username;
            return this;
        }

        public LoginBuilder password(String password) {
            this.password = password;
            return this;
        }

        public LoginBuilder usernameFieldID(String usernameFieldID) {
            this.usernameFieldID = usernameFieldID;
            return this;
        }

        public LoginBuilder passwordFieldID(String passwordFieldID) {
            this.passwordFieldID = passwordFieldID;
            return this;
        }

        public LoginBuilder entrypoint(String entrypoint) {
            this.entrypoint = entrypoint;
            return this;
        }

        public Login build() {
            return new Login(username, password, usernameFieldID, passwordFieldID, entrypoint);
        }

        public String toString() {
            return "Login.LoginBuilder(username=" + this.username + ", password=" + this.password + ", usernameFieldID=" + this.usernameFieldID + ", passwordFieldID=" + this.passwordFieldID + ", entrypoint=" + this.entrypoint + ")";
        }
    }
}
