package org.keycloak.workflow.model;

import java.util.ArrayList;
import java.util.List;

/** A single validation step inside a {@link WorkflowDefinition}. */
public class WorkflowStep {

    private String id;
    private int order;
    private String name;

    private Enums.StepMode mode = Enums.StepMode.SEQUENTIAL;

    private Enums.ApproverType approverType;
    /** user id, group id, role name or attribute name depending on approverType. */
    private String approverRef;

    /** SLA expressed in minutes. Business calendar of the parent definition applies. */
    private long slaMinutes;

    /** Reminder cadence in minutes. Empty => no reminders. */
    private List<Long> reminderOffsetsMinutes = new ArrayList<>();

    private List<Enums.NotificationChannel> channels = new ArrayList<>();
    /** Webhook URL when WEBHOOK channel is used. */
    private String webhookUrl;
    /** HMAC-SHA256 shared secret; when set, requests carry X-WF-Signature. */
    private String webhookSecret;

    /**
     * Optional condition; when it evaluates to true the step is skipped (marked
     * {@code SKIPPED}) and the engine moves on. Examples:
     * {@code risk &lt; 30}, {@code targetId == "low-impact"}, {@code attr.country == "FR"}.
     */
    private String skipCondition;

    private Enums.EscalationType escalationType = Enums.EscalationType.REASSIGN;
    private Enums.ApproverType escalationApproverType;
    private String escalationApproverRef;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Enums.StepMode getMode() { return mode; }
    public void setMode(Enums.StepMode m) { this.mode = m; }
    public Enums.ApproverType getApproverType() { return approverType; }
    public void setApproverType(Enums.ApproverType t) { this.approverType = t; }
    public String getApproverRef() { return approverRef; }
    public void setApproverRef(String r) { this.approverRef = r; }
    public long getSlaMinutes() { return slaMinutes; }
    public void setSlaMinutes(long m) { this.slaMinutes = m; }
    public List<Long> getReminderOffsetsMinutes() { return reminderOffsetsMinutes; }
    public void setReminderOffsetsMinutes(List<Long> r) { this.reminderOffsetsMinutes = r; }
    public List<Enums.NotificationChannel> getChannels() { return channels; }
    public void setChannels(List<Enums.NotificationChannel> c) { this.channels = c; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String w) { this.webhookUrl = w; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String s) { this.webhookSecret = s; }
    public String getSkipCondition() { return skipCondition; }
    public void setSkipCondition(String c) { this.skipCondition = c; }
    public Enums.EscalationType getEscalationType() { return escalationType; }
    public void setEscalationType(Enums.EscalationType e) { this.escalationType = e; }
    public Enums.ApproverType getEscalationApproverType() { return escalationApproverType; }
    public void setEscalationApproverType(Enums.ApproverType t) { this.escalationApproverType = t; }
    public String getEscalationApproverRef() { return escalationApproverRef; }
    public void setEscalationApproverRef(String r) { this.escalationApproverRef = r; }
}
