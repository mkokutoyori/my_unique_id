package org.keycloak.workflow.model;

import java.time.Instant;

/**
 * Approval delegation (out-of-office). While active, any approval addressed to
 * {@link #delegatorId} is also routed to {@link #delegateId}. SoD still applies
 * (the delegate cannot end up approving their own request).
 */
public class Delegation {

    private String id;
    private String realmId;
    private String delegatorId;
    private String delegateId;
    private Instant fromInstant;
    private Instant toInstant;
    private String reason;
    private boolean active = true;

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
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }

    public boolean coversNow() {
        Instant now = Instant.now();
        return active
                && (fromInstant == null || !now.isBefore(fromInstant))
                && (toInstant   == null || !now.isAfter(toInstant));
    }
}
