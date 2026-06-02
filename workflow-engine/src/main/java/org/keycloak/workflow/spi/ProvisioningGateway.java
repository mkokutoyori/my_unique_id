package org.keycloak.workflow.spi;

import org.keycloak.workflow.model.WorkflowInstance;

/**
 * Performs the actual role/group assignment in Keycloak once a workflow is fully
 * approved (FR-5.1). Decoupled so other targets (LDAP, SCIM, ...) can be plugged in.
 */
public interface ProvisioningGateway {

    /** @return true on success; false to mark the instance FAILED and audit. */
    boolean provision(WorkflowInstance instance);

    /**
     * Revoke a previously granted role/group (JIT expiry, manual cancellation,
     * re-certification refusal). Default implementation delegates to a sub-task.
     */
    boolean revoke(String realmId, String userId, String targetType, String targetId);
}
