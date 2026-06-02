package org.keycloak.workflow.jpa.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "WF_DELEGATION",
       indexes = {
           @Index(name = "ix_wfd_delegator", columnList = "realmId,delegatorId,active"),
           @Index(name = "ix_wfd_delegate",  columnList = "realmId,delegateId,active")
       })
public class DelegationEntity {

    @Id
    private String id;
    private String realmId;
    private String delegatorId;
    private String delegateId;
    private Instant fromInstant;
    private Instant toInstant;
    private boolean active;
    @Column(length = 500)
    private String reason;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRealmId() { return realmId; }
    public void setRealmId(String r) { this.realmId = r; }
    public String getDelegatorId() { return delegatorId; }
    public void setDelegatorId(String d) { this.delegatorId = d; }
    public String getDelegateId() { return delegateId; }
    public void setDelegateId(String d) { this.delegateId = d; }
    public Instant getFromInstant() { return fromInstant; }
    public void setFromInstant(Instant f) { this.fromInstant = f; }
    public Instant getToInstant() { return toInstant; }
    public void setToInstant(Instant t) { this.toInstant = t; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }
}
