package org.keycloak.workflow.engine.defaults;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.workflow.model.Enums;
import org.keycloak.workflow.spi.ApproverResolver;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class KeycloakApproverResolver implements ApproverResolver {

    private final KeycloakSession session;

    public KeycloakApproverResolver(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public List<String> resolve(String realmId, String requesterId,
                                Enums.ApproverType type, String ref) {
        RealmModel realm = session.realms().getRealm(realmId);
        if (realm == null || type == null || ref == null) return Collections.emptyList();
        return switch (type) {
            case SPECIFIC_USER -> {
                UserModel u = session.users().getUserById(realm, ref);
                yield u == null ? Collections.emptyList() : List.of(u.getId());
            }
            case GROUP -> {
                GroupModel g = realm.getGroupById(ref);
                if (g == null) yield Collections.emptyList();
                yield session.users().getGroupMembersStream(realm, g)
                        .map(UserModel::getId).collect(Collectors.toList());
            }
            case ROLE -> {
                RoleModel role = realm.getRole(ref);
                if (role == null) yield Collections.emptyList();
                yield session.users().getRoleMembersStream(realm, role)
                        .map(UserModel::getId).collect(Collectors.toList());
            }
            case MANAGER_ATTRIBUTE -> {
                UserModel requester = session.users().getUserById(realm, requesterId);
                if (requester == null) yield Collections.emptyList();
                String managerId = requester.getFirstAttribute(ref);
                if (managerId == null) yield Collections.emptyList();
                UserModel m = session.users().getUserById(realm, managerId);
                yield m == null ? Collections.emptyList() : List.of(m.getId());
            }
        };
    }
}
