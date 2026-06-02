package org.keycloak.workflow.spi;

import org.keycloak.workflow.model.Enums;
import org.keycloak.workflow.model.WorkflowInstance;
import org.keycloak.workflow.model.WorkflowStepInstance;

/** Multi-channel notifications (FR-3.3). */
public interface NotificationGateway {

    enum Kind { ASSIGNED, REMINDER, ESCALATED, COMPLETED, REJECTED }

    void notify(WorkflowInstance instance, WorkflowStepInstance step,
                Kind kind, Enums.NotificationChannel channel, String target);

    /** Webhook variant carrying the HMAC shared secret (may be null). */
    default void notify(WorkflowInstance instance, WorkflowStepInstance step,
                        Kind kind, Enums.NotificationChannel channel, String target, String secret) {
        notify(instance, step, kind, channel, target);
    }
}
