package br.com.vidasilva.jnventoryfx.repository;

import br.com.vidasilva.jnventoryfx.database.Database;
import br.com.vidasilva.jnventoryfx.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepository {

    public List<AuditLog> findRecent(int limit) {
        String sql = """
                SELECT id, event_time, actor_username, actor_email, actor_role, action, target_type, target_id, status, details
                FROM audit_logs
                ORDER BY id DESC
                LIMIT ?
                """;

        List<AuditLog> logs = new ArrayList<>();

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, Math.max(1, limit));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(mapAuditLog(resultSet));
                }
            }

            return logs;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load audit logs from SQLite.", exception);
        }
    }

    public void insert(
            String actorUsername,
            String actorEmail,
            String actorRole,
            String action,
            String targetType,
            String targetId,
            String status,
            String details
    ) {
        String sql = """
                INSERT INTO audit_logs (
                    actor_username,
                    actor_email,
                    actor_role,
                    action,
                    target_type,
                    target_id,
                    status,
                    details
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, actorUsername);
            statement.setString(2, actorEmail);
            statement.setString(3, actorRole);
            statement.setString(4, action);
            statement.setString(5, targetType);
            statement.setString(6, targetId);
            statement.setString(7, status);
            statement.setString(8, details);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not write audit log to SQLite.", exception);
        }

        Database.persistEncryptedCopy();
    }

    private AuditLog mapAuditLog(ResultSet resultSet) throws SQLException {
        return new AuditLog(
                resultSet.getInt("id"),
                resultSet.getString("event_time"),
                resultSet.getString("actor_username"),
                resultSet.getString("actor_email"),
                resultSet.getString("actor_role"),
                resultSet.getString("action"),
                resultSet.getString("target_type"),
                resultSet.getString("target_id"),
                resultSet.getString("status"),
                resultSet.getString("details")
        );
    }
}
