package org.keycloak.workflow.model;

import java.time.Instant;

/**
 * Pending revocation. The scheduler picks rows whose {@link #revokeAt} is due
 * and calls the provisioning gateway in reverse.
 */
public class RevocationJob {

    public enum Status { SCHEDULED, DONE, FAILED }

    private String id;
    private String realmId;
    private String instanceId;
    private String userId;
    private String targetType;
    private String targetId;
    private Instant revokeAt;
    private Status status = Status.SCHEDULED;
    private int attempts;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRealmId() { return realmId; }
    public void setRealmId(String r) { this.realmId = r; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String i) { this.instanceId = i; }
    public String getUserId() { return userId; }
    public void setUserId(String u) { this.userId = u; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String t) { this.targetType = t; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String t) { this.targetId = t; }
    public Instant getRevokeAt() { return revokeAt; }
    public void setRevokeAt(Instant r) { this.revokeAt = r; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int a) { this.attempts = a; }
}
