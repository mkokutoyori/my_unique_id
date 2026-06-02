package org.keycloak.workflow.engine;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.workflow.model.SodPolicy;

import java.util.List;
import java.util.Optional;

/**
 * Checks whether granting {@code targetType:targetId} to {@code userId} would
 * create a toxic combination with already-held entitlements.
 */
public class SodPolicyEvaluator {

    private final KeycloakSession session;
    private final List<SodPolicy> policies;

    public SodPolicyEvaluator(KeycloakSession session, List<SodPolicy> policies) {
        this.session = session;
        this.policies = policies;
    }

    public record Violation(String policyName, String conflictingTarget) {}

    public Optional<Violation> check(String realmId, String userId, String targetType, String targetId) {
        if (session == null || policies == null || policies.isEmpty()) return Optional.empty();
        RealmModel realm = session.realms().getRealm(realmId);
        if (realm == null) return Optional.empty();
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) return Optional.empty();

        for (SodPolicy p : policies) {
            if (!p.isActive()) continue;
            for (SodPolicy.Conflict c : p.getConflicts()) {
                if (!c.involves(targetType, targetId)) continue;
                String otherSide = c.otherSide(targetType, targetId);
                if (otherSide == null) continue;
                String[] split = otherSide.split(":", 2);
                if (split.length != 2) continue;
                if (userHolds(realm, user, split[0], split[1])) {
                    return Optional.of(new Violation(p.getName(), otherSide));
                }
            }
        }
        return Optional.empty();
    }

    private boolean userHolds(RealmModel realm, UserModel user, String type, String ref) {
        if ("ROLE".equalsIgnoreCase(type)) {
            RoleModel role = realm.getRole(ref);
            return role != null && user.hasRole(role);
        }
        if ("GROUP".equalsIgnoreCase(type)) {
            GroupModel group = realm.getGroupById(ref);
            return group != null && user.isMemberOf(group);
        }
        return false;
    }
}
