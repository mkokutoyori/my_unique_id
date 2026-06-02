package org.keycloak.workflow.engine.defaults;

import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.workflow.model.WorkflowInstance;
import org.keycloak.workflow.spi.ProvisioningGateway;

/** Grants the target role/group to the requester in Keycloak (FR-5.1). */
public class KeycloakProvisioningGateway implements ProvisioningGateway {

    private static final Logger LOG = Logger.getLogger(KeycloakProvisioningGateway.class);

    private final KeycloakSession session;

    public KeycloakProvisioningGateway(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean provision(WorkflowInstance inst) {
        RealmModel realm = session.realms().getRealm(inst.getRealmId());
        if (realm == null) return false;
        UserModel user = session.users().getUserById(realm, inst.getRequesterId());
        if (user == null) return false;
        try {
            if ("ROLE".equalsIgnoreCase(inst.getTargetType())) {
                RoleModel role = realm.getRole(inst.getTargetId());
                if (role == null) return false;
                user.grantRole(role);
                return true;
            }
            if ("GROUP".equalsIgnoreCase(inst.getTargetType())) {
                GroupModel group = realm.getGroupById(inst.getTargetId());
                if (group == null) return false;
                user.joinGroup(group);
                return true;
            }
        } catch (RuntimeException e) {
            LOG.errorf(e, "provisioning failed for instance %s", inst.getId());
            return false;
        }
        return false;
    }
}
