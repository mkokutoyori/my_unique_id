package org.keycloak.workflow.rest;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/** Mounts /realms/{realm}/workflow/* via Keycloak's RealmResourceProvider SPI. */
public class WorkflowRestResourceProviderFactory implements RealmResourceProviderFactory {

    public static final String ID = "workflow";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new RealmResourceProvider() {
            @Override public Object getResource() { return new WorkflowResource(session); }
            @Override public void close() { }
        };
    }

    @Override public void init(Config.Scope config) { }
    @Override public void postInit(KeycloakSessionFactory factory) { }
    @Override public void close() { }
    @Override public String getId() { return ID; }
}
