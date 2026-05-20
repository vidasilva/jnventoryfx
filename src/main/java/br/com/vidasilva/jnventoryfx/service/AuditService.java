package br.com.vidasilva.jnventoryfx.service;

import br.com.vidasilva.jnventoryfx.model.AuditLog;
import br.com.vidasilva.jnventoryfx.model.User;
import br.com.vidasilva.jnventoryfx.repository.AuditLogRepository;
import br.com.vidasilva.jnventoryfx.security.AuthorizationService;
import br.com.vidasilva.jnventoryfx.security.Permission;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AuditService {
    private static final AuditLogRepository AUDIT_LOG_REPOSITORY = new AuditLogRepository();
    private static final int DEFAULT_LIMIT = 200;

    private final ObservableList<AuditLog> recentLogs = FXCollections.observableArrayList();

    public ObservableList<AuditLog> getRecentLogs() {
        refreshRecentLogs();
        return recentLogs;
    }

    public void refreshRecentLogs() {
        AuthorizationService.require(Permission.VIEW_AUDIT_LOGS);
        recentLogs.clear();
        recentLogs.addAll(AUDIT_LOG_REPOSITORY.findRecent(DEFAULT_LIMIT));
    }

    public static void record(String action, String targetType, String targetId, String status, String details) {
        User currentUser = Session.getCurrentUser();
        recordForUser(currentUser, action, targetType, targetId, status, details);
    }

    public static void recordForUser(User user, String action, String targetType, String targetId, String status, String details) {
        String username = user == null ? "" : safe(user.getUsername());
        String email = user == null ? "" : safe(user.getEmail());
        String role = user == null || user.getRole() == null ? "" : user.getRole().name();
        write(username, email, role, action, targetType, targetId, status, details);
    }

    public static void recordAnonymous(String actorEmail, String action, String targetType, String targetId, String status, String details) {
        write("", safe(actorEmail), "", action, targetType, targetId, status, details);
    }

    private static void write(
            String actorUsername,
            String actorEmail,
            String actorRole,
            String action,
            String targetType,
            String targetId,
            String status,
            String details
    ) {
        try {
            AUDIT_LOG_REPOSITORY.insert(
                    safe(actorUsername),
                    safe(actorEmail),
                    safe(actorRole),
                    safe(action),
                    safe(targetType),
                    safe(targetId),
                    safe(status),
                    safe(details)
            );
        } catch (RuntimeException ignored) {
            // Audit logging must never break the business action it is observing.
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
