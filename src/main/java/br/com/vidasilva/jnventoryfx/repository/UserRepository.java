package br.com.vidasilva.jnventoryfx.repository;

import br.com.vidasilva.jnventoryfx.database.Database;
import br.com.vidasilva.jnventoryfx.model.User;
import br.com.vidasilva.jnventoryfx.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    public List<User> findAll() {
        String sql = """
                SELECT username, email, password, role
                FROM users
                ORDER BY username
                """;

        List<User> users = new ArrayList<>();

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }

            return users;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load users from SQLite.", exception);
        }
    }

    public User findByEmail(String email) {
        String sql = """
                SELECT username, email, password, role
                FROM users
                WHERE email = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapUser(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not find user in SQLite.", exception);
        }
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not check user email in SQLite.", exception);
        }
    }

    public User insert(String username, String email, String password, UserRole role) {
        String sql = """
                INSERT INTO users (username, email, password, role)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, password);
            statement.setString(4, role.name());
            statement.executeUpdate();

            return new User(username, email, password, role);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save user to SQLite.", exception);
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("password"),
                UserRole.valueOf(resultSet.getString("role"))
        );
    }
}
