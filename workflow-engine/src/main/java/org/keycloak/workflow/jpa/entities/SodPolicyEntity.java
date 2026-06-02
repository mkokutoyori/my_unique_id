package org.keycloak.workflow.jpa.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "WF_SOD_POLICY",
       indexes = { @Index(name = "ix_wfsod_realm", columnList = "realmId,active") })
public class SodPolicyEntity {

    @Id
    private String id;
    private String realmId;
    private String name;
    private boolean active;
    /** Serialized JSON array of conflicts. */
    @Lob @Column(name = "conflicts_json")
    private String conflictsJson;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRealmId() { return realmId; }
    public void setRealmId(String r) { this.realmId = r; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public String getConflictsJson() { return conflictsJson; }
    public void setConflictsJson(String c) { this.conflictsJson = c; }
}
