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
}
