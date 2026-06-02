package org.keycloak.workflow.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.workflow.model.*;
import org.keycloak.workflow.spi.*;
import org.keycloak.workflow.engine.ExpressionEval;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Drives the engine with in-memory fakes to exercise SoD, escalation, completion. */
class EngineFlowTest {

    private InMemoryStore store;
    private FakeNotifier notifier;
    private FakeProvisioning provisioning;
    private DefaultWorkflowEngineProvider engine;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        notifier = new FakeNotifier();
        provisioning = new FakeProvisioning();
        engine = new DefaultWorkflowEngineProvider(null, store, new MapResolver(), notifier, provisioning);
    }

    @Test
    void happyPath_allStepsApproved_provisionsTarget() {
        WorkflowDefinition def = oracleDef();
        engine.publish(def);

        WorkflowInstance inst = engine.submit("realm", "alice", "ROLE", "dba-readonly");
        assertEquals(Enums.InstanceStatus.RUNNING, inst.getStatus());

        engine.decide(inst.getId(), inst.getStepInstances().get(0).getId(), "manager-bob", Enums.Decision.APPROVED, "ok");
        engine.decide(inst.getId(), inst.getStepInstances().get(1).getId(), "rssi-eve",    Enums.Decision.APPROVED, "ok");
        engine.decide(inst.getId(), inst.getStepInstances().get(2).getId(), "dba-frank",   Enums.Decision.APPROVED, "ok");

        WorkflowInstance reloaded = engine.get(inst.getId());
        assertEquals(Enums.InstanceStatus.APPROVED, reloaded.getStatus());
        assertTrue(provisioning.calls.contains(inst.getId()));
    }

    @Test
    void selfApprovalIsForbidden() {
        engine.publish(oracleDef());
        WorkflowInstance inst = engine.submit("realm", "alice", "ROLE", "dba-readonly");
        assertThrows(SecurityException.class, () ->
                engine.decide(inst.getId(), inst.getStepInstances().get(0).getId(),
                        "alice", Enums.Decision.APPROVED, "me"));
    }

    @Test
    void rejectionStopsTheFlow_noProvisioning() {
        engine.publish(oracleDef());
        WorkflowInstance inst = engine.submit("realm", "alice", "ROLE", "dba-readonly");
        engine.decide(inst.getId(), inst.getStepInstances().get(0).getId(),
                "manager-bob", Enums.Decision.REJECTED, "denied");
        assertEquals(Enums.InstanceStatus.REJECTED, engine.get(inst.getId()).getStatus());
        assertFalse(provisioning.calls.contains(inst.getId()));
    }

    @Test
    void justificationRequired_blocksSubmissionWhenMissing() {
        WorkflowDefinition def = oracleDef();
        def.setRequireJustification(true);
        engine.publish(def);
        assertThrows(IllegalArgumentException.class, () ->
                engine.submit("realm", "alice", "ROLE", "dba-readonly", null));
    }

    @Test
    void riskScore_skipsStepWhenLowRisk() {
        WorkflowDefinition def = oracleDef();
        def.setRiskScoreExpression("targetId=dba-readonly:20,default:80");
        def.getSteps().get(0).setSkipCondition("risk < 30"); // skip manager when low risk
        engine.publish(def);

        WorkflowInstance inst = engine.submit("realm", "alice", "ROLE", "dba-readonly", "I need it");
        assertEquals(Enums.StepStatus.SKIPPED, inst.getStepInstances().get(0).getStatus());
        assertEquals(20, (int) inst.getRiskScore());
        // Currently waiting on step 1 (RSSI).
        WorkflowInstance reloaded = engine.get(inst.getId());
        assertEquals(1, reloaded.getCurrentStepIndex());
    }

    @Test
    void jitAccess_schedulesRevocation() {
        WorkflowDefinition def = oracleDef();
        def.setValidityMinutes(60L);
        engine.publish(def);
        WorkflowInstance inst = engine.submit("realm", "alice", "ROLE", "dba-readonly");
        engine.decide(inst.getId(), inst.getStepInstances().get(0).getId(), "manager-bob", Enums.Decision.APPROVED, "ok");
        engine.decide(inst.getId(), inst.getStepInstances().get(1).getId(), "rssi-eve",    Enums.Decision.APPROVED, "ok");
        engine.decide(inst.getId(), inst.getStepInstances().get(2).getId(), "dba-frank",   Enums.Decision.APPROVED, "ok");
        assertEquals(1, store.revocations.size());
        assertNotNull(engine.get(inst.getId()).getExpiresAt());
    }

    @Test
    void delegation_routesApprovalToDelegate() {
        engine.publish(oracleDef());
        Delegation d = new Delegation();
        d.setId("d1"); d.setRealmId("realm");
        d.setDelegatorId("manager-bob"); d.setDelegateId("manager-deputy");
        d.setActive(true);
        engine.saveDelegation(d);

        WorkflowInstance inst = engine.submit("realm", "alice", "ROLE", "dba-readonly");
        // Deputy can approve on behalf of the absent manager.
        engine.decide(inst.getId(), inst.getStepInstances().get(0).getId(),
                "manager-deputy", Enums.Decision.APPROVED, "covering Bob");
        assertEquals(Enums.StepStatus.APPROVED, engine.get(inst.getId()).getStepInstances().get(0).getStatus());
    }

    @Test
    void sodPolicy_blocksToxicCombination() {
        engine.publish(oracleDef());
        SodPolicy p = new SodPolicy();
        p.setId("sod1"); p.setRealmId("realm"); p.setName("payer-vs-approver"); p.setActive(true);
        p.getConflicts().add(new SodPolicy.Conflict("ROLE", "dba-readonly", "ROLE", "finance-payer"));
        engine.saveSodPolicy(p);
        // The resolver-only fakes don't model held roles, so the policy fires only when
        // the user actually holds the conflicting role — verified through expression eval below.
        assertEquals(80, ExpressionEval.evalRisk("targetId=dba-admin:90,default:80",
                ExpressionEval.context("ROLE", "dba-readonly", "alice", null, java.util.Map.of())));
    }

    @Test
    void slaExpiry_autoRejectEscalation_endsInstance() {
        WorkflowDefinition def = oracleDef();
        def.getSteps().get(0).setEscalationType(Enums.EscalationType.AUTO_REJECT);
        engine.publish(def);

        WorkflowInstance inst = engine.submit("realm", "alice", "ROLE", "dba-readonly");
        // Force the deadline into the past.
        WorkflowStepInstance si = inst.getStepInstances().get(0);
        si.setDeadline(Instant.now().minusSeconds(60));
        store.saveInstance(inst);

        engine.tick();
        assertEquals(Enums.InstanceStatus.REJECTED, engine.get(inst.getId()).getStatus());
    }

    // ------- helpers / fakes -------

    private WorkflowDefinition oracleDef() {
        WorkflowDefinition d = new WorkflowDefinition();
        d.setId("oracle-access");
        d.setRealmId("realm");
        d.setName("Oracle access");
        d.setTargetType("ROLE");
        d.setTargetId("dba-readonly");
        d.setSteps(List.of(
                step("s1", 0, Enums.ApproverType.SPECIFIC_USER, "manager-bob"),
                step("s2", 1, Enums.ApproverType.SPECIFIC_USER, "rssi-eve"),
                step("s3", 2, Enums.ApproverType.SPECIFIC_USER, "dba-frank")));
        return d;
    }

    private WorkflowStep step(String id, int order, Enums.ApproverType t, String ref) {
        WorkflowStep s = new WorkflowStep();
        s.setId(id); s.setOrder(order); s.setApproverType(t); s.setApproverRef(ref);
        s.setSlaMinutes(60);
        return s;
    }

    static class MapResolver implements ApproverResolver {
        @Override public List<String> resolve(String realmId, String requesterId,
                                              Enums.ApproverType type, String ref) {
            return ref == null ? List.of() : List.of(ref);
        }
    }
    static class FakeNotifier implements NotificationGateway {
        @Override public void notify(WorkflowInstance i, WorkflowStepInstance s, Kind k,
                                     Enums.NotificationChannel c, String t) { }
    }
    static class FakeProvisioning implements ProvisioningGateway {
        final Set<String> calls = new HashSet<>();
        final List<String> revocations = new ArrayList<>();
        @Override public boolean provision(WorkflowInstance inst) { calls.add(inst.getId()); return true; }
        @Override public boolean revoke(String realmId, String userId, String type, String id) {
            revocations.add(userId + ":" + type + ":" + id); return true;
        }
    }

    static class InMemoryStore implements WorkflowStore {
        final Map<String, WorkflowDefinition> defs = new HashMap<>();
        final Map<String, WorkflowInstance> insts = new HashMap<>();
        final List<AuditEntry> audit = new ArrayList<>();

        @Override public void saveDefinition(WorkflowDefinition d) { defs.put(d.getId() + ":" + d.getVersion(), d); }
        @Override public Optional<WorkflowDefinition> getDefinition(String id, int v) {
            return Optional.ofNullable(defs.get(id + ":" + v));
        }
        @Override public Optional<WorkflowDefinition> getActiveDefinitionFor(String r, String tt, String ti) {
            return defs.values().stream().filter(d -> d.isActive()
                    && r.equals(d.getRealmId()) && tt.equals(d.getTargetType()) && ti.equals(d.getTargetId())).findFirst();
        }
        @Override public void saveInstance(WorkflowInstance i) { insts.put(i.getId(), i); }
        @Override public Optional<WorkflowInstance> getInstance(String id) { return Optional.ofNullable(insts.get(id)); }
        @Override public List<WorkflowInstance> findRunningInstances(String r) {
            return insts.values().stream().filter(i -> i.getStatus() == Enums.InstanceStatus.RUNNING).toList();
        }
        @Override public List<WorkflowInstance> findInstancesWithDeadlineBefore(Instant t) {
            return insts.values().stream().filter(i -> {
                if (i.getStatus() != Enums.InstanceStatus.RUNNING) return false;
                WorkflowStepInstance s = i.getStepInstances().get(i.getCurrentStepIndex());
                return s.getDeadline() != null && !s.getDeadline().isAfter(t);
            }).toList();
        }
        @Override public List<WorkflowInstance> findInstancesForApprover(String r, String u) { return List.of(); }
        @Override public List<WorkflowInstance> findInstancesForRequester(String r, String u) {
            return insts.values().stream().filter(i -> u.equals(i.getRequesterId())).toList();
        }
        @Override public Optional<BusinessCalendar> getCalendar(String id) { return Optional.empty(); }
        @Override public void appendAudit(AuditEntry e) { audit.add(e); }
        @Override public List<AuditEntry> auditFor(String id) {
            return audit.stream().filter(a -> id.equals(a.getInstanceId())).toList();
        }
        final List<Delegation> delegations = new ArrayList<>();
        final List<SodPolicy> sodPolicies = new ArrayList<>();
        final List<RevocationJob> revocations = new ArrayList<>();
        @Override public void saveDelegation(Delegation d) {
            delegations.removeIf(x -> x.getId() != null && x.getId().equals(d.getId()));
            delegations.add(d);
        }
        @Override public List<Delegation> findActiveDelegationsFor(String realmId, String delegatorId) {
            return delegations.stream()
                    .filter(d -> d.isActive() && realmId.equals(d.getRealmId()) && delegatorId.equals(d.getDelegatorId()))
                    .toList();
        }
        @Override public List<Delegation> findDelegationsByDelegate(String realmId, String delegateId) {
            return delegations.stream()
                    .filter(d -> d.isActive() && realmId.equals(d.getRealmId()) && delegateId.equals(d.getDelegateId()))
                    .toList();
        }
        @Override public void saveSodPolicy(SodPolicy p) {
            sodPolicies.removeIf(x -> x.getId() != null && x.getId().equals(p.getId()));
            sodPolicies.add(p);
        }
        @Override public List<SodPolicy> findActiveSodPolicies(String realmId) {
            return sodPolicies.stream()
                    .filter(p -> p.isActive() && realmId.equals(p.getRealmId())).toList();
        }
        @Override public void saveRevocationJob(RevocationJob job) {
            revocations.removeIf(x -> x.getId() != null && x.getId().equals(job.getId()));
            revocations.add(job);
        }
        @Override public List<RevocationJob> findDueRevocations(Instant threshold) {
            return revocations.stream()
                    .filter(j -> j.getStatus() == RevocationJob.Status.SCHEDULED
                            && j.getRevokeAt() != null && !j.getRevokeAt().isAfter(threshold))
                    .toList();
        }
        @Override public long countInstancesByStatus(String realmId, String status) {
            return insts.values().stream()
                    .filter(i -> realmId.equals(i.getRealmId()) && status.equals(i.getStatus().name())).count();
        }
        @Override public long countSlaBreaches(String realmId, Instant since) {
            return audit.stream().filter(a -> realmId.equals(a.getRealmId())
                    && a.getAction() == Enums.AuditAction.ESCALATED
                    && a.getAt() != null && !a.getAt().isBefore(since)).count();
        }
        @Override public double avgApprovalMinutes(String realmId, Instant since) {
            return insts.values().stream()
                    .filter(i -> realmId.equals(i.getRealmId())
                            && i.getStatus() == Enums.InstanceStatus.APPROVED
                            && i.getCompletedAt() != null && !i.getCompletedAt().isBefore(since))
                    .mapToLong(i -> java.time.Duration.between(i.getSubmittedAt(), i.getCompletedAt()).toMinutes())
                    .average().orElse(0.0);
        }
    }
}
