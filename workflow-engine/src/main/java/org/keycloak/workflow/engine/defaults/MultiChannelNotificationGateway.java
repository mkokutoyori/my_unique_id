package org.keycloak.workflow.engine.defaults;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.workflow.model.Enums;
import org.keycloak.workflow.model.WorkflowInstance;
import org.keycloak.workflow.model.WorkflowStepInstance;
import org.keycloak.workflow.spi.NotificationGateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Email via Keycloak's EmailTemplateProvider + Webhook via JDK HttpClient (FR-3.3). */
public class MultiChannelNotificationGateway implements NotificationGateway {

    private static final Logger LOG = Logger.getLogger(MultiChannelNotificationGateway.class);

    private final KeycloakSession session;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public MultiChannelNotificationGateway(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void notify(WorkflowInstance inst, WorkflowStepInstance step,
                       Kind kind, Enums.NotificationChannel channel, String target) {
        if (channel == Enums.NotificationChannel.EMAIL) {
            sendEmail(inst, step, kind, target);
        } else if (channel == Enums.NotificationChannel.WEBHOOK) {
            sendWebhook(inst, step, kind, target);
        }
    }

    private void sendEmail(WorkflowInstance inst, WorkflowStepInstance si, Kind kind, String userId) {
        try {
            RealmModel realm = session.realms().getRealm(inst.getRealmId());
            UserModel user = session.users().getUserById(realm, userId);
            if (user == null || user.getEmail() == null) return;
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("instanceId", inst.getId());
            attrs.put("stepName", si.getStepId());
            attrs.put("targetType", inst.getTargetType());
            attrs.put("targetId", inst.getTargetId());
            attrs.put("deadline", String.valueOf(si.getDeadline()));
            attrs.put("kind", kind.name());
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .setUser(user)
                    .send("workflowApprovalSubject", "workflow-approval.ftl", attrs);
        } catch (EmailException e) {
            LOG.warnf(e, "email notification failed (instance=%s)", inst.getId());
        }
    }

    private void sendWebhook(WorkflowInstance inst, WorkflowStepInstance si, Kind kind, String url) {
        if (url == null || url.isBlank()) return;
        String body = String.format(
                "{\"instanceId\":\"%s\",\"stepId\":\"%s\",\"kind\":\"%s\",\"target\":\"%s:%s\",\"deadline\":\"%s\"}",
                inst.getId(), si.getStepId(), kind, inst.getTargetType(), inst.getTargetId(), si.getDeadline());
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                LOG.warnf("webhook returned %d for instance %s", resp.statusCode(), inst.getId());
            }
        } catch (Exception e) {
            LOG.warnf(e, "webhook delivery failed (instance=%s)", inst.getId());
        }
    }
}
