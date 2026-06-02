package org.keycloak.workflow.engine.defaults;

import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.workflow.model.Enums;
import org.keycloak.workflow.model.WorkflowInstance;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes workflow transitions onto Keycloak's event bus so they appear in
 * the admin event store and propagate to SIEM listeners (Splunk, ELK, ...).
 */
public class KeycloakEventEmitter {

    private final KeycloakSession session;

    public KeycloakEventEmitter(KeycloakSession session) { this.session = session; }

    public void emit(WorkflowInstance inst, Enums.AuditAction action, String details) {
        EventStoreProvider store = session.getProvider(EventStoreProvider.class);
        if (store == null) return;
        Event event = new Event();
        event.setTime(System.currentTimeMillis());
        event.setType(EventType.CUSTOM_REQUIRED_ACTION);
        event.setRealmId(inst.getRealmId());
        event.setUserId(inst.getRequesterId());
        Map<String, String> details_ = new HashMap<>();
        details_.put("workflow_action", action.name());
        details_.put("workflow_instance", inst.getId());
        details_.put("target", inst.getTargetType() + ":" + inst.getTargetId());
        if (details != null) details_.put("info", details);
        if (inst.getRiskScore() != null) details_.put("risk", String.valueOf(inst.getRiskScore()));
        event.setDetails(details_);
        store.onEvent(event);
    }
}
