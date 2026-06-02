package org.keycloak.workflow.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;
import org.keycloak.workflow.jpa.entities.WorkflowAuditEntity;
import org.keycloak.workflow.jpa.entities.WorkflowDefinitionEntity;
import org.keycloak.workflow.jpa.entities.WorkflowInstanceEntity;
import org.keycloak.workflow.model.AuditEntry;
import org.keycloak.workflow.model.BusinessCalendar;
import org.keycloak.workflow.model.Enums;
import org.keycloak.workflow.model.WorkflowDefinition;
import org.keycloak.workflow.model.WorkflowInstance;
import org.keycloak.workflow.model.WorkflowStep;
import org.keycloak.workflow.model.WorkflowStepInstance;
import org.keycloak.workflow.spi.WorkflowStore;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JPA-backed store. Steps and step instances are serialized as JSON to keep schema simple. */
public class JpaWorkflowStore implements WorkflowStore {

    private final KeycloakSession session;

    public JpaWorkflowStore(KeycloakSession session) {
        this.session = session;
    }

    private EntityManager em() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    // ----- Definitions -----

    @Override
    public void saveDefinition(WorkflowDefinition def) {
        String pk = def.getId() + ":" + def.getVersion();
        WorkflowDefinitionEntity e = em().find(WorkflowDefinitionEntity.class, pk);
        if (e == null) { e = new WorkflowDefinitionEntity(); e.setPk(pk); }
        e.setDefId(def.getId());
        e.setVersion(def.getVersion());
        e.setRealmId(def.getRealmId());
        e.setName(def.getName());
        e.setTargetType(def.getTargetType());
        e.setTargetId(def.getTargetId());
        e.setActive(def.isActive());
        e.setFallbackGroupId(def.getFallbackGroupId());
        e.setCalendarId(def.getCalendarId());
        e.setStepsJson(writeJson(def.getSteps()));
        em().merge(e);
    }

    @Override
    public Optional<WorkflowDefinition> getDefinition(String id, int version) {
        WorkflowDefinitionEntity e = em().find(WorkflowDefinitionEntity.class, id + ":" + version);
        return Optional.ofNullable(e).map(this::toDef);
    }

    @Override
    public Optional<WorkflowDefinition> getActiveDefinitionFor(String realmId, String targetType, String targetId) {
        TypedQuery<WorkflowDefinitionEntity> q = em().createQuery(
                "select d from WorkflowDefinitionEntity d where d.realmId=:r and d.targetType=:tt and d.targetId=:ti and d.active=true",
                WorkflowDefinitionEntity.class)
                .setParameter("r", realmId).setParameter("tt", targetType).setParameter("ti", targetId);
        return q.getResultStream().findFirst().map(this::toDef);
    }

    private WorkflowDefinition toDef(WorkflowDefinitionEntity e) {
        WorkflowDefinition d = new WorkflowDefinition();
        d.setId(e.getDefId());
        d.setVersion(e.getVersion());
        d.setRealmId(e.getRealmId());
        d.setName(e.getName());
        d.setTargetType(e.getTargetType());
        d.setTargetId(e.getTargetId());
        d.setActive(e.isActive());
        d.setFallbackGroupId(e.getFallbackGroupId());
        d.setCalendarId(e.getCalendarId());
        d.setSteps(readJson(e.getStepsJson(), WorkflowStep[].class));
        return d;
    }

    // ----- Instances -----

    @Override
    public void saveInstance(WorkflowInstance inst) {
        WorkflowInstanceEntity e = em().find(WorkflowInstanceEntity.class, inst.getId());
        if (e == null) { e = new WorkflowInstanceEntity(); e.setId(inst.getId()); }
        e.setRealmId(inst.getRealmId());
        e.setDefinitionId(inst.getDefinitionId());
        e.setDefinitionVersion(inst.getDefinitionVersion());
        e.setRequesterId(inst.getRequesterId());
        e.setTargetType(inst.getTargetType());
        e.setTargetId(inst.getTargetId());
        e.setStatus(inst.getStatus().name());
        e.setSubmittedAt(inst.getSubmittedAt());
        e.setCompletedAt(inst.getCompletedAt());
        e.setCurrentStepIndex(inst.getCurrentStepIndex());
        if (inst.getStatus() == Enums.InstanceStatus.RUNNING
                && inst.getCurrentStepIndex() < inst.getStepInstances().size()) {
            e.setCurrentDeadline(inst.getStepInstances().get(inst.getCurrentStepIndex()).getDeadline());
        } else {
            e.setCurrentDeadline(null);
        }
        e.setStepsJson(writeJson(inst.getStepInstances()));
        em().merge(e);
    }

    @Override
    public Optional<WorkflowInstance> getInstance(String id) {
        return Optional.ofNullable(em().find(WorkflowInstanceEntity.class, id)).map(this::toInstance);
    }

    @Override
    public List<WorkflowInstance> findRunningInstances(String realmId) {
        return em().createQuery(
                "select i from WorkflowInstanceEntity i where i.realmId=:r and i.status='RUNNING'",
                WorkflowInstanceEntity.class)
                .setParameter("r", realmId)
                .getResultStream().map(this::toInstance).toList();
    }

    @Override
    public List<WorkflowInstance> findInstancesWithDeadlineBefore(Instant threshold) {
        return em().createQuery(
                "select i from WorkflowInstanceEntity i where i.status='RUNNING' and i.currentDeadline<=:t",
                WorkflowInstanceEntity.class)
                .setParameter("t", threshold)
                .getResultStream().map(this::toInstance).toList();
    }

    @Override
    public List<WorkflowInstance> findInstancesForApprover(String realmId, String approverUserId) {
        // Naive scan; production impl would index assignees in a side table.
        return findRunningInstances(realmId).stream()
                .filter(i -> {
                    WorkflowStepInstance s = i.getStepInstances().get(i.getCurrentStepIndex());
                    return approverUserId.equals(s.getCurrentAssigneeRef());
                }).toList();
    }

    @Override
    public List<WorkflowInstance> findInstancesForRequester(String realmId, String requesterId) {
        return em().createQuery(
                "select i from WorkflowInstanceEntity i where i.realmId=:r and i.requesterId=:u",
                WorkflowInstanceEntity.class)
                .setParameter("r", realmId).setParameter("u", requesterId)
                .getResultStream().map(this::toInstance).toList();
    }

    private WorkflowInstance toInstance(WorkflowInstanceEntity e) {
        WorkflowInstance i = new WorkflowInstance();
        i.setId(e.getId());
        i.setRealmId(e.getRealmId());
        i.setDefinitionId(e.getDefinitionId());
        i.setDefinitionVersion(e.getDefinitionVersion());
        i.setRequesterId(e.getRequesterId());
        i.setTargetType(e.getTargetType());
        i.setTargetId(e.getTargetId());
        i.setStatus(Enums.InstanceStatus.valueOf(e.getStatus()));
        i.setSubmittedAt(e.getSubmittedAt());
        i.setCompletedAt(e.getCompletedAt());
        i.setCurrentStepIndex(e.getCurrentStepIndex());
        i.setStepInstances(readJson(e.getStepsJson(), WorkflowStepInstance[].class));
        return i;
    }

    // ----- Audit (append-only) -----

    @Override
    public void appendAudit(AuditEntry entry) {
        WorkflowAuditEntity a = new WorkflowAuditEntity();
        a.setId(entry.getId());
        a.setRealmId(entry.getRealmId());
        a.setInstanceId(entry.getInstanceId());
        a.setStepInstanceId(entry.getStepInstanceId());
        a.setAt(entry.getAt());
        a.setActorId(entry.getActorId());
        a.setAction(entry.getAction().name());
        a.setDetails(entry.getDetails());
        em().persist(a);
    }

    @Override
    public List<AuditEntry> auditFor(String instanceId) {
        return em().createQuery(
                "select a from WorkflowAuditEntity a where a.instanceId=:i order by a.at asc",
                WorkflowAuditEntity.class)
                .setParameter("i", instanceId)
                .getResultStream().map(this::toAudit).toList();
    }

    private AuditEntry toAudit(WorkflowAuditEntity a) {
        AuditEntry e = new AuditEntry();
        e.setId(a.getId());
        e.setRealmId(a.getRealmId());
        e.setInstanceId(a.getInstanceId());
        e.setStepInstanceId(a.getStepInstanceId());
        e.setAt(a.getAt());
        e.setActorId(a.getActorId());
        e.setAction(Enums.AuditAction.valueOf(a.getAction()));
        e.setDetails(a.getDetails());
        return e;
    }

    @Override
    public Optional<BusinessCalendar> getCalendar(String id) {
        // Calendars are loaded from realm attributes / a dedicated table in a follow-up.
        return Optional.empty();
    }

    // ----- JSON helpers -----

    private String writeJson(Object o) {
        try { return JsonSerialization.writeValueAsString(o); }
        catch (IOException ex) { throw new RuntimeException(ex); }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> readJson(String json, Class<T[]> type) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            T[] arr = JsonSerialization.readValue(json, type);
            return new ArrayList<>(java.util.Arrays.asList(arr));
        } catch (IOException ex) { throw new RuntimeException(ex); }
    }
}
