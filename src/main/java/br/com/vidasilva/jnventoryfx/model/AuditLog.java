package br.com.vidasilva.jnventoryfx.model;

public class AuditLog {
    private final int id;
    private final String eventTime;
    private final String actorUsername;
    private final String actorEmail;
    private final String actorRole;
    private final String action;
    private final String targetType;
    private final String targetId;
    private final String status;
    private final String details;

    public AuditLog(
            int id,
            String eventTime,
            String actorUsername,
            String actorEmail,
            String actorRole,
            String action,
            String targetType,
            String targetId,
            String status,
            String details
    ) {
        this.id = id;
        this.eventTime = eventTime;
        this.actorUsername = actorUsername;
        this.actorEmail = actorEmail;
        this.actorRole = actorRole;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.status = status;
        this.details = details;
    }

    public int getId() {
        return id;
    }

    public String getEventTime() {
        return eventTime;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getActorRole() {
        return actorRole;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getStatus() {
        return status;
    }

    public String getDetails() {
        return details;
    }
}
