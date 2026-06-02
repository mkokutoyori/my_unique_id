package org.keycloak.workflow.engine;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.workflow.engine.defaults.KeycloakApproverResolver;
import org.keycloak.workflow.engine.defaults.KeycloakProvisioningGateway;
import org.keycloak.workflow.engine.defaults.MultiChannelNotificationGateway;
import org.keycloak.workflow.jpa.JpaWorkflowStore;
import org.keycloak.workflow.scheduler.SlaScheduler;
import org.keycloak.workflow.spi.WorkflowEngineProvider;
import org.keycloak.workflow.spi.WorkflowEngineProviderFactory;

public class DefaultWorkflowEngineProviderFactory implements WorkflowEngineProviderFactory {

    public static final String ID = "default";

    private SlaScheduler scheduler;

    @Override
    public WorkflowEngineProvider create(KeycloakSession session) {
        return new DefaultWorkflowEngineProvider(
                session,
                new JpaWorkflowStore(session),
                new KeycloakApproverResolver(session),
                new MultiChannelNotificationGateway(session),
                new KeycloakProvisioningGateway(session));
    }

    @Override
    public void init(Config.Scope config) { /* tunables read here */ }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NFR-1: dedicated background scheduler so SLA processing doesn't block auth flows.
        scheduler = new SlaScheduler(factory);
        scheduler.start();
    }

    @Override
    public void close() {
        if (scheduler != null) scheduler.stop();
    }

    @Override public String getId() { return ID; }
}
