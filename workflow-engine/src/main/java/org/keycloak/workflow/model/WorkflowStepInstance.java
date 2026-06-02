package org.keycloak.workflow.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WorkflowStepInstance {

    private String id;
    private String instanceId;
    private String stepId;
    private int order;

    private Enums.StepStatus status = Enums.StepStatus.PENDING;
    private Instant startedAt;
    private Instant deadline;
    private Instant decidedAt;

    /** Effective assignee resolved at runtime (user id or group id). */
    private String currentAssigneeRef;
    private Enums.ApproverType currentAssigneeType;

    /** Approver who acted. */
    private String approverId;
    private String comment;

    /** Reminder offsets already fired (idempotency). */
    private List<Long> firedReminders = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String i) { this.instanceId = i; }
    public String getStepId() { return stepId; }
    public void setStepId(String s) { this.stepId = s; }
    public int getOrder() { return order; }
    public void setOrder(int o) { this.order = o; }
    public Enums.StepStatus getStatus() { return status; }
    public void setStatus(Enums.StepStatus s) { this.status = s; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant s) { this.startedAt = s; }
    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant d) { this.deadline = d; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant d) { this.decidedAt = d; }
    public String getCurrentAssigneeRef() { return currentAssigneeRef; }
    public void setCurrentAssigneeRef(String c) { this.currentAssigneeRef = c; }
    public Enums.ApproverType getCurrentAssigneeType() { return currentAssigneeType; }
    public void setCurrentAssigneeType(Enums.ApproverType t) { this.currentAssigneeType = t; }
    public String getApproverId() { return approverId; }
    public void setApproverId(String a) { this.approverId = a; }
    public String getComment() { return comment; }
    public void setComment(String c) { this.comment = c; }
    public List<Long> getFiredReminders() { return firedReminders; }
    public void setFiredReminders(List<Long> r) { this.firedReminders = r; }
}
