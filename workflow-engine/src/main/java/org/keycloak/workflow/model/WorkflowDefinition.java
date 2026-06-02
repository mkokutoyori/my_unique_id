package org.keycloak.workflow.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Versioned workflow blueprint. Versioning (edge case "modification while running")
 * is enforced by snapshotting the active version id onto each {@link WorkflowInstance}.
 */
public class WorkflowDefinition {

    private String id;
    private String realmId;
    private String name;
    private int version;
    private boolean active;

    /** Target object the workflow protects: a Keycloak role or group. */
    private String targetType; // ROLE | GROUP
    private String targetId;

    private List<WorkflowStep> steps = new ArrayList<>();

    /** Fallback approver group used when an assignee cannot be resolved (FR cas limite #1). */
    private String fallbackGroupId;

    /** Business calendar id; null => 24/7. */
    private String calendarId;

    /** JIT access: validity in minutes after provisioning; null/0 => permanent. */
    private Long validityMinutes;

    /** Reject the request if the requester does not provide a justification. */
    private boolean requireJustification;

    /**
     * Optional expression evaluated against the request context to produce a numeric
     * risk score (0–100). Used by {@link WorkflowStep#getSkipCondition()}.
     * Supported tokens: {@code target}, {@code requester}, {@code attr.<name>}.
     */
    private String riskScoreExpression;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRealmId() { return realmId; }
    public void setRealmId(String realmId) { this.realmId = realmId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public List<WorkflowStep> getSteps() { return steps; }
    public void setSteps(List<WorkflowStep> steps) { this.steps = steps; }
    public String getFallbackGroupId() { return fallbackGroupId; }
    public void setFallbackGroupId(String g) { this.fallbackGroupId = g; }
    public String getCalendarId() { return calendarId; }
    public void setCalendarId(String c) { this.calendarId = c; }
    public Long getValidityMinutes() { return validityMinutes; }
    public void setValidityMinutes(Long v) { this.validityMinutes = v; }
    public boolean isRequireJustification() { return requireJustification; }
    public void setRequireJustification(boolean r) { this.requireJustification = r; }
    public String getRiskScoreExpression() { return riskScoreExpression; }
    public void setRiskScoreExpression(String e) { this.riskScoreExpression = e; }
}
