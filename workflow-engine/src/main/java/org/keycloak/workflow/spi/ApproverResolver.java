package org.keycloak.workflow.spi;

import org.keycloak.workflow.model.Enums;

import java.util.List;

/** Resolves an approver descriptor (user, group, role, attribute) to concrete user ids. */
public interface ApproverResolver {

    /**
     * @return ordered list of candidate user ids; empty list triggers the
     *         fallback group rule (cas limite "manager absent").
     */
    List<String> resolve(String realmId, String requesterId,
                         Enums.ApproverType type, String ref);
}
