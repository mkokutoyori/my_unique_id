package org.keycloak.workflow.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class WorkflowEngineSpi implements Spi {
    @Override public boolean isInternal() { return false; }
    @Override public String getName() { return "workflow-engine"; }
    @Override public Class<? extends Provider> getProviderClass() { return WorkflowEngineProvider.class; }
    @Override public Class<? extends ProviderFactory> getProviderFactoryClass() { return WorkflowEngineProviderFactory.class; }
}
