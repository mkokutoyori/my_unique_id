package org.keycloak.workflow.spi;

import org.keycloak.provider.Provider;
import org.keycloak.workflow.model.WorkflowDefinition;
import org.keycloak.workflow.model.WorkflowInstance;
import org.keycloak.workflow.model.Enums;

import java.util.List;

/**
 * Core engine API. All mutating methods must be called inside a Keycloak
 * transaction (handled by services) so state is persisted atomically (NFR-3).
 */
public interface WorkflowEngineProvider extends Provider {

    // --- Designer (FR-1) ---
    WorkflowDefinition publish(WorkflowDefinition def);
    WorkflowDefinition getActiveDefinitionFor(String realmId, String targetType, String targetId);
    WorkflowDefinition getDefinition(String id, int version);

    // --- Request lifecycle ---
    WorkflowInstance submit(String realmId, String requesterId, String targetType, String targetId);
    WorkflowInstance get(String instanceId);
    List<WorkflowInstance> listForApprover(String realmId, String approverUserId);
    List<WorkflowInstance> listForRequester(String realmId, String requesterId);

    /** Approve / reject the current step (FR-5, FR-6.2 SoD enforced here). */
    WorkflowInstance decide(String instanceId, String stepInstanceId,
                            String actorUserId, Enums.Decision decision, String comment);

    /** Cancel a pending request (requester only). */
    void cancel(String instanceId, String actorUserId, String reason);

    // --- Background tick (called by scheduler, NFR-1) ---
    /** Process due SLA deadlines and reminders. Idempotent; safe to call repeatedly. */
    void tick();
}
