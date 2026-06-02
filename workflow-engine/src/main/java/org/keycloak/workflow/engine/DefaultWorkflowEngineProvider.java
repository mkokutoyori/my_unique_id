package org.keycloak.workflow.engine;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.workflow.model.AuditEntry;
import org.keycloak.workflow.model.BusinessCalendar;
import org.keycloak.workflow.model.Enums;
import org.keycloak.workflow.model.WorkflowDefinition;
import org.keycloak.workflow.model.WorkflowInstance;
import org.keycloak.workflow.model.WorkflowStep;
import org.keycloak.workflow.model.WorkflowStepInstance;
import org.keycloak.workflow.spi.ApproverResolver;
import org.keycloak.workflow.spi.NotificationGateway;
import org.keycloak.workflow.spi.ProvisioningGateway;
import org.keycloak.workflow.spi.WorkflowEngineProvider;
import org.keycloak.workflow.spi.WorkflowStore;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Default engine implementation. State changes go through the {@link WorkflowStore}
 * so the process survives restarts (NFR-3). All actions append an immutable
 * {@link AuditEntry} (FR-6.1).
 */
public class DefaultWorkflowEngineProvider implements WorkflowEngineProvider {

    private static final Logger LOG = Logger.getLogger(DefaultWorkflowEngineProvider.class);

    private final KeycloakSession session;
    private final WorkflowStore store;
    private final ApproverResolver resolver;
    private final NotificationGateway notifier;
    private final ProvisioningGateway provisioning;

    public DefaultWorkflowEngineProvider(KeycloakSession session, WorkflowStore store,
                                         ApproverResolver resolver, NotificationGateway notifier,
                                         ProvisioningGateway provisioning) {
        this.session = session;
        this.store = store;
        this.resolver = resolver;
        this.notifier = notifier;
        this.provisioning = provisioning;
    }

    // ---------- Designer (FR-1) ----------

    @Override
    public WorkflowDefinition publish(WorkflowDefinition def) {
        // Process versioning (edge case #3): each publish bumps the version.
        Optional<WorkflowDefinition> current = store.getActiveDefinitionFor(
                def.getRealmId(), def.getTargetType(), def.getTargetId());
        int nextVersion = current.map(d -> d.getVersion() + 1).orElse(1);
        current.ifPresent(c -> { c.setActive(false); store.saveDefinition(c); });
        if (def.getId() == null) def.setId(UUID.randomUUID().toString());
        def.setVersion(nextVersion);
        def.setActive(true);
        store.saveDefinition(def);
        return def;
    }

    @Override
    public WorkflowDefinition getActiveDefinitionFor(String realmId, String targetType, String targetId) {
        return store.getActiveDefinitionFor(realmId, targetType, targetId).orElse(null);
    }

    @Override
    public WorkflowDefinition getDefinition(String id, int version) {
        return store.getDefinition(id, version).orElse(null);
    }

    // ---------- Submission ----------

    @Override
    public WorkflowInstance submit(String realmId, String requesterId, String targetType, String targetId) {
        WorkflowDefinition def = store.getActiveDefinitionFor(realmId, targetType, targetId)
                .orElseThrow(() -> new IllegalStateException("No active workflow for " + targetType + ":" + targetId));

        WorkflowInstance inst = new WorkflowInstance();
        inst.setId(UUID.randomUUID().toString());
        inst.setRealmId(realmId);
        inst.setDefinitionId(def.getId());
        inst.setDefinitionVersion(def.getVersion()); // snapshot version
        inst.setRequesterId(requesterId);
        inst.setTargetType(targetType);
        inst.setTargetId(targetId);
        inst.setSubmittedAt(Instant.now());
        inst.setStatus(Enums.InstanceStatus.RUNNING);

        for (WorkflowStep step : def.getSteps()) {
            WorkflowStepInstance si = new WorkflowStepInstance();
            si.setId(UUID.randomUUID().toString());
            si.setInstanceId(inst.getId());
            si.setStepId(step.getId());
            si.setOrder(step.getOrder());
            inst.getStepInstances().add(si);
        }
        inst.setCurrentStepIndex(0);
        store.saveInstance(inst);
        audit(inst, null, requesterId, Enums.AuditAction.SUBMITTED, "request submitted");

        activateStep(inst, def, 0);
        return inst;
    }

    private void activateStep(WorkflowInstance inst, WorkflowDefinition def, int index) {
        if (index >= def.getSteps().size()) {
            complete(inst);
            return;
        }
        WorkflowStep step = def.getSteps().get(index);
        WorkflowStepInstance si = inst.getStepInstances().get(index);

        // Resolve assignees (cas limite "manager absent" => fallback group).
        List<String> assignees = resolver.resolve(inst.getRealmId(), inst.getRequesterId(),
                step.getApproverType(), step.getApproverRef());
        if (assignees.isEmpty() && def.getFallbackGroupId() != null) {
            si.setCurrentAssigneeType(Enums.ApproverType.GROUP);
            si.setCurrentAssigneeRef(def.getFallbackGroupId());
        } else {
            si.setCurrentAssigneeType(step.getApproverType());
            si.setCurrentAssigneeRef(step.getApproverRef());
        }

        // SoD: if the requester is the sole resolved approver, escalate immediately (FR-6.2 + cas limite #2).
        if (assignees.size() == 1 && assignees.get(0).equals(inst.getRequesterId())) {
            audit(inst, si, inst.getRequesterId(), Enums.AuditAction.SOD_BLOCKED,
                    "requester is sole approver; auto-escalating");
            applyEscalation(inst, def, index, "SoD: requester is sole approver");
            return;
        }

        si.setStatus(Enums.StepStatus.PENDING);
        si.setStartedAt(Instant.now());
        si.setDeadline(computeDeadline(def, step, si.getStartedAt()));
        inst.setCurrentStepIndex(index);
        store.saveInstance(inst);
        audit(inst, si, null, Enums.AuditAction.STEP_STARTED, "assigned to " + si.getCurrentAssigneeRef());
        notifyStep(inst, si, def, step, NotificationGateway.Kind.ASSIGNED);
    }

    private Instant computeDeadline(WorkflowDefinition def, WorkflowStep step, Instant from) {
        if (def.getCalendarId() == null) {
            return from.plusSeconds(step.getSlaMinutes() * 60);
        }
        BusinessCalendar cal = store.getCalendar(def.getCalendarId()).orElse(null);
        if (cal == null) return from.plusSeconds(step.getSlaMinutes() * 60);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(from, cal.getZoneId());
        return cal.addBusinessMinutes(zdt, step.getSlaMinutes()).toInstant();
    }

    // ---------- Decision (FR-5, FR-6.2) ----------

    @Override
    public WorkflowInstance decide(String instanceId, String stepInstanceId,
                                   String actorUserId, Enums.Decision decision, String comment) {
        WorkflowInstance inst = store.getInstance(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("unknown instance"));
        if (inst.getStatus() != Enums.InstanceStatus.RUNNING) {
            throw new IllegalStateException("instance not running");
        }

        WorkflowStepInstance si = inst.getStepInstances().stream()
                .filter(s -> s.getId().equals(stepInstanceId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown step"));
        if (si.getStatus() != Enums.StepStatus.PENDING) {
            throw new IllegalStateException("step not pending");
        }

        // SoD enforcement (FR-6.2): requester cannot approve own request.
        if (actorUserId.equals(inst.getRequesterId())) {
            audit(inst, si, actorUserId, Enums.AuditAction.SOD_BLOCKED, "self-approval rejected");
            throw new SecurityException("self-approval is forbidden (SoD)");
        }
        if (!isAuthorizedApprover(inst, si, actorUserId)) {
            throw new SecurityException("actor is not an authorized approver for this step");
        }

        si.setApproverId(actorUserId);
        si.setComment(comment);
        si.setDecidedAt(Instant.now());

        WorkflowDefinition def = store.getDefinition(inst.getDefinitionId(), inst.getDefinitionVersion())
                .orElseThrow();

        if (decision == Enums.Decision.REJECTED) {
            si.setStatus(Enums.StepStatus.REJECTED);
            inst.setStatus(Enums.InstanceStatus.REJECTED);
            inst.setCompletedAt(Instant.now());
            store.saveInstance(inst);
            audit(inst, si, actorUserId, Enums.AuditAction.REJECTED, comment);
            notifyStep(inst, si, def, def.getSteps().get(si.getOrder()), NotificationGateway.Kind.REJECTED);
            return inst;
        }

        si.setStatus(Enums.StepStatus.APPROVED);
        store.saveInstance(inst);
        audit(inst, si, actorUserId, Enums.AuditAction.APPROVED, comment);

        activateStep(inst, def, si.getOrder() + 1);
        return store.getInstance(instanceId).orElse(inst);
    }

    private boolean isAuthorizedApprover(WorkflowInstance inst, WorkflowStepInstance si, String actorUserId) {
        List<String> candidates = resolver.resolve(inst.getRealmId(), inst.getRequesterId(),
                si.getCurrentAssigneeType(), si.getCurrentAssigneeRef());
        return candidates.contains(actorUserId);
    }

    @Override
    public void cancel(String instanceId, String actorUserId, String reason) {
        WorkflowInstance inst = store.getInstance(instanceId).orElseThrow();
        if (!actorUserId.equals(inst.getRequesterId())) {
            throw new SecurityException("only the requester can cancel");
        }
        if (inst.getStatus() != Enums.InstanceStatus.RUNNING) return;
        inst.setStatus(Enums.InstanceStatus.CANCELLED);
        inst.setCompletedAt(Instant.now());
        store.saveInstance(inst);
        audit(inst, null, actorUserId, Enums.AuditAction.CANCELLED, reason);
    }

    // ---------- Completion + provisioning (FR-5.1) ----------

    private void complete(WorkflowInstance inst) {
        inst.setStatus(Enums.InstanceStatus.APPROVED);
        inst.setCompletedAt(Instant.now());
        store.saveInstance(inst);
        audit(inst, null, null, Enums.AuditAction.COMPLETED, "all steps approved");

        boolean ok = false;
        try {
            ok = provisioning.provision(inst);
        } catch (RuntimeException e) {
            LOG.errorf(e, "provisioning failed for instance %s", inst.getId());
        }
        if (ok) {
            audit(inst, null, null, Enums.AuditAction.PROVISIONING_OK,
                    "granted " + inst.getTargetType() + ":" + inst.getTargetId());
        } else {
            inst.setStatus(Enums.InstanceStatus.FAILED);
            store.saveInstance(inst);
            audit(inst, null, null, Enums.AuditAction.PROVISIONING_FAILED, "provisioning step failed");
        }
    }

    // ---------- Background processing: SLA + reminders + escalation (FR-2/3/4) ----------

    @Override
    public void tick() {
        Instant now = Instant.now();
        // Look slightly ahead to fire reminders configured before deadline.
        List<WorkflowInstance> due = store.findInstancesWithDeadlineBefore(now.plusSeconds(60));
        for (WorkflowInstance inst : due) {
            if (inst.getStatus() != Enums.InstanceStatus.RUNNING) continue;
            WorkflowStepInstance si = inst.getStepInstances().get(inst.getCurrentStepIndex());
            if (si.getStatus() != Enums.StepStatus.PENDING) continue;

            WorkflowDefinition def = store.getDefinition(inst.getDefinitionId(), inst.getDefinitionVersion()).orElse(null);
            if (def == null) continue;
            WorkflowStep step = def.getSteps().get(si.getOrder());

            if (!si.getDeadline().isAfter(now)) {
                applyEscalation(inst, def, si.getOrder(), "SLA exceeded");
            } else {
                fireDueReminders(inst, si, def, step, now);
            }
        }
    }

    private void fireDueReminders(WorkflowInstance inst, WorkflowStepInstance si,
                                  WorkflowDefinition def, WorkflowStep step, Instant now) {
        for (Long offsetMin : step.getReminderOffsetsMinutes()) {
            Instant fireAt = si.getDeadline().minusSeconds(offsetMin * 60);
            if (!fireAt.isAfter(now) && !si.getFiredReminders().contains(offsetMin)) {
                si.getFiredReminders().add(offsetMin);
                store.saveInstance(inst);
                audit(inst, si, null, Enums.AuditAction.REMINDED, "reminder T-" + offsetMin + "m");
                notifyStep(inst, si, def, step, NotificationGateway.Kind.REMINDER);
            }
        }
    }

    private void applyEscalation(WorkflowInstance inst, WorkflowDefinition def, int index, String reason) {
        WorkflowStep step = def.getSteps().get(index);
        WorkflowStepInstance si = inst.getStepInstances().get(index);
        switch (step.getEscalationType()) {
            case AUTO_APPROVE -> {
                si.setStatus(Enums.StepStatus.AUTO_APPROVED);
                store.saveInstance(inst);
                audit(inst, si, null, Enums.AuditAction.ESCALATED, reason + "; auto-approve");
                activateStep(inst, def, index + 1);
            }
            case AUTO_REJECT -> {
                si.setStatus(Enums.StepStatus.AUTO_REJECTED);
                inst.setStatus(Enums.InstanceStatus.REJECTED);
                inst.setCompletedAt(Instant.now());
                store.saveInstance(inst);
                audit(inst, si, null, Enums.AuditAction.ESCALATED, reason + "; auto-reject");
                notifyStep(inst, si, def, step, NotificationGateway.Kind.REJECTED);
            }
            case REASSIGN -> {
                si.setStatus(Enums.StepStatus.ESCALATED);
                si.setCurrentAssigneeType(step.getEscalationApproverType());
                si.setCurrentAssigneeRef(step.getEscalationApproverRef());
                // New SLA window starts now.
                si.setStartedAt(Instant.now());
                si.setDeadline(computeDeadline(def, step, si.getStartedAt()));
                si.setFiredReminders(new ArrayList<>());
                si.setStatus(Enums.StepStatus.PENDING);
                store.saveInstance(inst);
                audit(inst, si, null, Enums.AuditAction.REASSIGNED,
                        reason + "; reassigned to " + step.getEscalationApproverRef());
                notifyStep(inst, si, def, step, NotificationGateway.Kind.ESCALATED);
            }
        }
    }

    // ---------- Queries ----------

    @Override
    public WorkflowInstance get(String instanceId) {
        return store.getInstance(instanceId).orElse(null);
    }

    @Override
    public List<WorkflowInstance> listForApprover(String realmId, String approverUserId) {
        return store.findInstancesForApprover(realmId, approverUserId);
    }

    @Override
    public List<WorkflowInstance> listForRequester(String realmId, String requesterId) {
        return store.findInstancesForRequester(realmId, requesterId);
    }

    // ---------- Helpers ----------

    private void notifyStep(WorkflowInstance inst, WorkflowStepInstance si,
                            WorkflowDefinition def, WorkflowStep step,
                            NotificationGateway.Kind kind) {
        if (notifier == null) return;
        List<String> recipients = resolver.resolve(inst.getRealmId(), inst.getRequesterId(),
                si.getCurrentAssigneeType(), si.getCurrentAssigneeRef());
        for (Enums.NotificationChannel ch : step.getChannels()) {
            if (ch == Enums.NotificationChannel.WEBHOOK) {
                notifier.notify(inst, si, kind, ch, step.getWebhookUrl());
            } else {
                for (String userId : recipients) {
                    notifier.notify(inst, si, kind, ch, userId);
                }
            }
        }
    }

    private void audit(WorkflowInstance inst, WorkflowStepInstance si, String actorId,
                       Enums.AuditAction action, String details) {
        AuditEntry e = new AuditEntry();
        e.setId(UUID.randomUUID().toString());
        e.setRealmId(inst.getRealmId());
        e.setInstanceId(inst.getId());
        e.setStepInstanceId(si != null ? si.getId() : null);
        e.setAt(Instant.now());
        e.setActorId(actorId);
        e.setAction(action);
        e.setDetails(details);
        store.appendAudit(e);
    }

    @Override public void close() { /* no-op; session manages transactions */ }
}
