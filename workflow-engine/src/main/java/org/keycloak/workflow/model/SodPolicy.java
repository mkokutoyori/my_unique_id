package org.keycloak.workflow.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Toxic-combination Separation of Duty policy: declares pairs of targets that
 * must not be held by the same user simultaneously (e.g. "finance-payer" +
 * "finance-approver"). Evaluated at submission and just before provisioning.
 */
public class SodPolicy {

    private String id;
    private String realmId;
    private String name;
    private boolean active = true;

    /** Pairs of conflicting targets. Order within a pair is irrelevant. */
    private List<Conflict> conflicts = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRealmId() { return realmId; }
    public void setRealmId(String r) { this.realmId = r; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public List<Conflict> getConflicts() { return conflicts; }
    public void setConflicts(List<Conflict> c) { this.conflicts = c; }

    public static class Conflict {
        private String typeA;
        private String refA;
        private String typeB;
        private String refB;

        public Conflict() {}
        public Conflict(String typeA, String refA, String typeB, String refB) {
            this.typeA = typeA; this.refA = refA; this.typeB = typeB; this.refB = refB;
        }
        public String getTypeA() { return typeA; }
        public void setTypeA(String t) { this.typeA = t; }
        public String getRefA() { return refA; }
        public void setRefA(String r) { this.refA = r; }
        public String getTypeB() { return typeB; }
        public void setTypeB(String t) { this.typeB = t; }
        public String getRefB() { return refB; }
        public void setRefB(String r) { this.refB = r; }

        public boolean involves(String type, String ref) {
            return (type.equals(typeA) && ref.equals(refA))
                || (type.equals(typeB) && ref.equals(refB));
        }
        public String otherSide(String type, String ref) {
            if (type.equals(typeA) && ref.equals(refA)) return typeB + ":" + refB;
            if (type.equals(typeB) && ref.equals(refB)) return typeA + ":" + refA;
            return null;
        }
    }
}
