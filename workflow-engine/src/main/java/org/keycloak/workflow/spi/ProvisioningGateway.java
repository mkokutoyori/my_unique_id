package org.keycloak.workflow.spi;

import org.keycloak.workflow.model.WorkflowInstance;

/**
 * Performs the actual role/group assignment in Keycloak once a workflow is fully
 * approved (FR-5.1). Decoupled so other targets (LDAP, SCIM, ...) can be plugged in.
 */
public interface ProvisioningGateway {

    /** @return true on success; false to mark the instance FAILED and audit. */
    boolean provision(WorkflowInstance instance);
}
