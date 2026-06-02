package org.keycloak.workflow.jpa;

import org.keycloak.Config;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class WorkflowJpaEntityProviderFactory implements JpaEntityProviderFactory {

    public static final String ID = "workflow-engine";

    @Override public JpaEntityProvider create(KeycloakSession session) { return new WorkflowJpaEntityProvider(); }
    @Override public void init(Config.Scope c) { }
    @Override public void postInit(KeycloakSessionFactory f) { }
    @Override public void close() { }
    @Override public String getId() { return ID; }
}
