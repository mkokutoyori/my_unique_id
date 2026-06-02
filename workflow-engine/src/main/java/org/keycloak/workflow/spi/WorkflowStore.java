package org.keycloak.workflow.spi;

import org.keycloak.workflow.model.AuditEntry;
import org.keycloak.workflow.model.BusinessCalendar;
import org.keycloak.workflow.model.Delegation;
import org.keycloak.workflow.model.RevocationJob;
import org.keycloak.workflow.model.SodPolicy;
import org.keycloak.workflow.model.WorkflowDefinition;
import org.keycloak.workflow.model.WorkflowInstance;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence abstraction. Default implementation is JPA-backed
 * (see {@code org.keycloak.workflow.jpa}) so workflow state survives restarts (NFR-3).
 */
public interface WorkflowStore {

    // Definitions
    void saveDefinition(WorkflowDefinition def);
    Optional<WorkflowDefinition> getDefinition(String id, int version);
    Optional<WorkflowDefinition> getActiveDefinitionFor(String realmId, String targetType, String targetId);

    // Instances
    void saveInstance(WorkflowInstance instance);
    Optional<WorkflowInstance> getInstance(String id);
    List<WorkflowInstance> findRunningInstances(String realmId);
    /** Running instances whose current step deadline is on or before {@code threshold}. */
    List<WorkflowInstance> findInstancesWithDeadlineBefore(Instant threshold);
    List<WorkflowInstance> findInstancesForApprover(String realmId, String approverUserId);
    List<WorkflowInstance> findInstancesForRequester(String realmId, String requesterId);

    // Calendars
    Optional<BusinessCalendar> getCalendar(String id);

    // Audit (append-only)
    void appendAudit(AuditEntry entry);
    List<AuditEntry> auditFor(String instanceId);

    // Delegations
    void saveDelegation(Delegation d);
    List<Delegation> findActiveDelegationsFor(String realmId, String delegatorId);
    List<Delegation> findDelegationsByDelegate(String realmId, String delegateId);

    // SoD policies
    void saveSodPolicy(SodPolicy p);
    List<SodPolicy> findActiveSodPolicies(String realmId);

    // Revocation jobs (JIT)
    void saveRevocationJob(RevocationJob job);
    List<RevocationJob> findDueRevocations(Instant threshold);

    // Aggregate metrics
    long countInstancesByStatus(String realmId, String status);
    long countSlaBreaches(String realmId, Instant since);
    double avgApprovalMinutes(String realmId, Instant since);
}
