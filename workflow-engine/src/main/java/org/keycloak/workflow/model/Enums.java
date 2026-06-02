package org.keycloak.workflow.model;

public class Enums {

    public enum StepMode { SEQUENTIAL, PARALLEL_ANY, PARALLEL_ALL }

    public enum ApproverType {
        SPECIFIC_USER,
        GROUP,
        ROLE,
        MANAGER_ATTRIBUTE
    }

    public enum Decision { APPROVED, REJECTED }

    public enum StepStatus { PENDING, APPROVED, REJECTED, ESCALATED, AUTO_APPROVED, AUTO_REJECTED, SKIPPED }

    public enum InstanceStatus { RUNNING, APPROVED, REJECTED, CANCELLED, FAILED }

    public enum EscalationType { REASSIGN, AUTO_APPROVE, AUTO_REJECT }

    public enum NotificationChannel { EMAIL, WEBHOOK }

    public enum AuditAction {
        SUBMITTED, STEP_STARTED, APPROVED, REJECTED,
        REMINDED, ESCALATED, REASSIGNED, COMPLETED, CANCELLED,
        SOD_BLOCKED, PROVISIONING_OK, PROVISIONING_FAILED
    }
}
