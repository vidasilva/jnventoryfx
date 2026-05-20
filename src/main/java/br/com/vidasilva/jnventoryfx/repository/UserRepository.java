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
                SELECT username, email, password, role, must_change_password
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
                SELECT username, email, password, role, must_change_password
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

    public User insert(String username, String email, String passwordHash, UserRole role) {
        return insert(username, email, passwordHash, role, false);
    }

    public User insert(String username, String email, String passwordHash, UserRole role, boolean mustChangePassword) {
        String sql = """
                INSERT INTO users (username, email, password, role, must_change_password)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.setString(4, role.name());
            statement.setInt(5, mustChangePassword ? 1 : 0);
            statement.executeUpdate();

            Database.persistEncryptedCopy();
            return new User(username, email, passwordHash, role, mustChangePassword);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save user to SQLite.", exception);
        }
    }

    public void updatePasswordHash(String email, String passwordHash) {
        updatePasswordHash(email, passwordHash, null);
    }

    public void updatePasswordHash(String email, String passwordHash, Boolean mustChangePassword) {
        String sql = mustChangePassword == null
                ? """
                UPDATE users
                SET password = ?
                WHERE email = ?
                """
                : """
                UPDATE users
                SET password = ?,
                    must_change_password = ?
                WHERE email = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, passwordHash);

            if (mustChangePassword == null) {
                statement.setString(2, email);
            } else {
                statement.setInt(2, mustChangePassword ? 1 : 0);
                statement.setString(3, email);
            }

            statement.executeUpdate();
            Database.persistEncryptedCopy();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update user password hash in SQLite.", exception);
        }
    }

    public void updateMustChangePassword(String email, boolean mustChangePassword) {
        String sql = """
                UPDATE users
                SET must_change_password = ?
                WHERE email = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, mustChangePassword ? 1 : 0);
            statement.setString(2, email);
            statement.executeUpdate();
            Database.persistEncryptedCopy();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update first-login password flag in SQLite.", exception);
        }
    }

    public void savePasswordResetToken(String email, String tokenHash, String expiresAt) {
        String sql = """
                INSERT INTO password_reset_tokens (user_email, token_hash, expires_at)
                VALUES (?, ?, ?)
                ON CONFLICT(user_email) DO UPDATE SET
                    token_hash = excluded.token_hash,
                    expires_at = excluded.expires_at,
                    created_at = CURRENT_TIMESTAMP
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            statement.setString(2, tokenHash);
            statement.setString(3, expiresAt);
            statement.executeUpdate();
            Database.persistEncryptedCopy();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save password reset token in SQLite.", exception);
        }
    }

    public PasswordResetToken findPasswordResetToken(String email) {
        String sql = """
                SELECT user_email, token_hash, expires_at
                FROM password_reset_tokens
                WHERE user_email = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return new PasswordResetToken(
                        resultSet.getString("user_email"),
                        resultSet.getString("token_hash"),
                        resultSet.getString("expires_at")
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load password reset token from SQLite.", exception);
        }
    }

    public void clearPasswordResetToken(String email) {
        String sql = """
                DELETE FROM password_reset_tokens
                WHERE user_email = ?
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            statement.executeUpdate();
            Database.persistEncryptedCopy();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not clear password reset token from SQLite.", exception);
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("password"),
                UserRole.valueOf(resultSet.getString("role")),
                resultSet.getInt("must_change_password") == 1
        );
    }

    public record PasswordResetToken(String userEmail, String tokenHash, String expiresAt) {
    }
}
