package br.com.vidasilva.jnventoryfx.service;

import br.com.vidasilva.jnventoryfx.model.User;
import br.com.vidasilva.jnventoryfx.model.UserRole;
import br.com.vidasilva.jnventoryfx.repository.UserRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class UserService {

    private static final UserRepository USER_REPOSITORY = new UserRepository();
    private static final ObservableList<User> USERS = FXCollections.observableArrayList(USER_REPOSITORY.findAll());

    public ObservableList<User> getUsers() {
        return USERS;
    }

    public boolean registerUser(String username, String email, String password, String confirmPassword) {
        return registerUser(username, email, password, confirmPassword, UserRole.CASHIER);
    }

    public boolean registerUser(String username, String email, String password, String confirmPassword, UserRole role) {
        if (isBlank(username) || isBlank(email) || isBlank(password) || isBlank(confirmPassword) || role == null) {
            return false;
        }

        if (!password.equals(confirmPassword)) {
            return false;
        }

        String normalizedEmail = normalizeEmail(email);

        if (USER_REPOSITORY.existsByEmail(normalizedEmail)) {
            return false;
        }

        User user = USER_REPOSITORY.insert(username.trim(), normalizedEmail, password, role);
        USERS.add(user);
        return true;
    }

    public boolean signIn(String email, String password) {
        return authenticateUser(email, password) != null;
    }

    public User authenticateUser(String email, String password) {
        if (isBlank(email) || isBlank(password)) {
            return null;
        }

        User user = USER_REPOSITORY.findByEmail(normalizeEmail(email));

        if (user == null || !user.getPassword().equals(password)) {
            return null;
        }

        return user;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
