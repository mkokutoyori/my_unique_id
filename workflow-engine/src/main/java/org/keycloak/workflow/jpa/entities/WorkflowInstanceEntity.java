package org.keycloak.workflow.jpa.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "WF_INSTANCE",
       indexes = {
           @Index(name = "ix_wfi_deadline", columnList = "currentDeadline,status"),
           @Index(name = "ix_wfi_requester", columnList = "realmId,requesterId"),
           @Index(name = "ix_wfi_status",   columnList = "realmId,status")
       })
public class WorkflowInstanceEntity {

    @Id
    private String id;
    private String realmId;
    private String definitionId;
    private int definitionVersion;
    private String requesterId;
    private String targetType;
    private String targetId;
    private String status;
    private Instant submittedAt;
    private Instant completedAt;
    private int currentStepIndex;
    /** Denormalized cursor used by the SLA scheduler. */
    private Instant currentDeadline;
    @Column(length = 2000)
    private String justification;
    private Integer riskScore;
    private Instant expiresAt;

    /** Serialized JSON of step instances. */
    @Lob @Column(name = "steps_json")
    private String stepsJson;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRealmId() { return realmId; }
    public void setRealmId(String r) { this.realmId = r; }
    public String getDefinitionId() { return definitionId; }
    public void setDefinitionId(String d) { this.definitionId = d; }
    public int getDefinitionVersion() { return definitionVersion; }
    public void setDefinitionVersion(int v) { this.definitionVersion = v; }
    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String r) { this.requesterId = r; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String t) { this.targetType = t; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String t) { this.targetId = t; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant s) { this.submittedAt = s; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant c) { this.completedAt = c; }
    public int getCurrentStepIndex() { return currentStepIndex; }
    public void setCurrentStepIndex(int i) { this.currentStepIndex = i; }
    public Instant getCurrentDeadline() { return currentDeadline; }
    public void setCurrentDeadline(Instant d) { this.currentDeadline = d; }
    public String getStepsJson() { return stepsJson; }
    public void setStepsJson(String s) { this.stepsJson = s; }
    public String getJustification() { return justification; }
    public void setJustification(String j) { this.justification = j; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer r) { this.riskScore = r; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant e) { this.expiresAt = e; }
}
