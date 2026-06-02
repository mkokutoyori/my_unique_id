package org.keycloak.workflow.jpa.entities;

import jakarta.persistence.*;

/** Persisted definition. Composite uniqueness on (id, version) supports process versioning. */
@Entity
@Table(name = "WF_DEFINITION",
       indexes = { @Index(name = "ix_wfdef_target", columnList = "realmId,targetType,targetId,active") })
public class WorkflowDefinitionEntity {

    @Id
    private String pk; // id + ":" + version
    private String defId;
    private int version;
    private String realmId;
    private String name;
    private String targetType;
    private String targetId;
    private boolean active;
    private String fallbackGroupId;
    private String calendarId;

    /** Serialized JSON of steps. */
    @Lob @Column(name = "steps_json")
    private String stepsJson;

    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }
    public String getDefId() { return defId; }
    public void setDefId(String d) { this.defId = d; }
    public int getVersion() { return version; }
    public void setVersion(int v) { this.version = v; }
    public String getRealmId() { return realmId; }
    public void setRealmId(String r) { this.realmId = r; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String t) { this.targetType = t; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String t) { this.targetId = t; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public String getFallbackGroupId() { return fallbackGroupId; }
    public void setFallbackGroupId(String f) { this.fallbackGroupId = f; }
    public String getCalendarId() { return calendarId; }
    public void setCalendarId(String c) { this.calendarId = c; }
    public String getStepsJson() { return stepsJson; }
    public void setStepsJson(String s) { this.stepsJson = s; }
}
