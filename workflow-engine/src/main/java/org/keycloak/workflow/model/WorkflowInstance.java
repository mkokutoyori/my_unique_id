package org.keycloak.workflow.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Live run of a {@link WorkflowDefinition}. Bound to the definition version that
 * was active when the request was submitted (process versioning, edge case #3).
 */
public class WorkflowInstance {

    private String id;
    private String realmId;
    private String definitionId;
    private int definitionVersion;

    private String requesterId;
    private String targetType;
    private String targetId;

    private Enums.InstanceStatus status = Enums.InstanceStatus.RUNNING;
    private Instant submittedAt;
    private Instant completedAt;

    private int currentStepIndex;
    private List<WorkflowStepInstance> stepInstances = new ArrayList<>();

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
    public Enums.InstanceStatus getStatus() { return status; }
    public void setStatus(Enums.InstanceStatus s) { this.status = s; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant s) { this.submittedAt = s; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant c) { this.completedAt = c; }
    public int getCurrentStepIndex() { return currentStepIndex; }
    public void setCurrentStepIndex(int i) { this.currentStepIndex = i; }
    public List<WorkflowStepInstance> getStepInstances() { return stepInstances; }
    public void setStepInstances(List<WorkflowStepInstance> s) { this.stepInstances = s; }
}
