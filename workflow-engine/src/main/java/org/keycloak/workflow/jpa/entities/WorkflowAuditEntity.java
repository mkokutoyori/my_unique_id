package org.keycloak.workflow.jpa.entities;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Audit row. Application layer only INSERTs (FR-6.1: immutability).
 * Schema migration is expected to GRANT INSERT only on this table.
 */
@Entity
@Table(name = "WF_AUDIT", indexes = { @Index(name = "ix_wfa_instance", columnList = "instanceId,at") })
public class WorkflowAuditEntity {

    @Id
    private String id;
    private String realmId;
    private String instanceId;
    private String stepInstanceId;
    private Instant at;
    private String actorId;
    private String action;
    @Column(length = 2000)
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
    public String getAction() { return action; }
    public void setAction(String a) { this.action = a; }
    public String getDetails() { return details; }
    public void setDetails(String d) { this.details = d; }
}
