package org.keycloak.workflow.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.workflow.jpa.entities.WorkflowAuditEntity;
import org.keycloak.workflow.jpa.entities.WorkflowDefinitionEntity;
import org.keycloak.workflow.jpa.entities.WorkflowInstanceEntity;

import java.util.Arrays;
import java.util.List;

/** Registers workflow entities with Keycloak's shared persistence unit. */
public class WorkflowJpaEntityProvider implements JpaEntityProvider {

    @Override
    public List<Class<?>> getEntities() {
        return Arrays.asList(
                WorkflowDefinitionEntity.class,
                WorkflowInstanceEntity.class,
                WorkflowAuditEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/workflow-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return WorkflowJpaEntityProviderFactory.ID;
    }

    @Override public void close() { }
}
