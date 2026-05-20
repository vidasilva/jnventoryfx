package br.com.vidasilva.jnventoryfx.model;

public class User {
    private String username;
    private String email;
    private String passwordHash;
    private UserRole role;
    private boolean mustChangePassword;

    public User(String username, String email, String passwordHash) {
        this(username, email, passwordHash, UserRole.CASHIER, false);
    }

    public User(String username, String email, String passwordHash, UserRole role) {
        this(username, email, passwordHash, role, false);
    }

    public User(String username, String email, String passwordHash, UserRole role, boolean mustChangePassword) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
