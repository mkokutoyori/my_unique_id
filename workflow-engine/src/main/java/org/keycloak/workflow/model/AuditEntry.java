package org.keycloak.workflow.model;

import java.time.Instant;

/** Immutable audit record (FR-6.1). Persistence layer must reject UPDATE/DELETE. */
public class AuditEntry {

    private String id;
    private String realmId;
    private String instanceId;
    private String stepInstanceId;
    private Instant at;
    private String actorId;
    private Enums.AuditAction action;
    private String details;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRealmId() { return realmId; }
    public void setRealmId(String r) { this.realmId = r; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String i) { this.instanceId = i; }
    public String getStepInstanceId() { return stepInstanceId; }
    public void setStepInstanceId(String s) { this.stepInstanceId = s; }
    public Instant getAt() { return at; }
    public void setAt(Instant a) { this.at = a; }
    public String getActorId() { return actorId; }
    public void setActorId(String a) { this.actorId = a; }
    public Enums.AuditAction getAction() { return action; }
    public void setAction(Enums.AuditAction a) { this.action = a; }
    public String getDetails() { return details; }
    public void setDetails(String d) { this.details = d; }
}
