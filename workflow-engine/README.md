# Keycloak Workflow Engine

Approval workflow engine for sensitive role/group assignments. Implements the
BRD "Moteur d'Approbation de Flux de Travail" as a decoupled Keycloak SPI
extension (NFR-2) with JPA-backed state (NFR-3) and a background SLA worker
running on Keycloak's cluster-aware timer (NFR-1).

## Mapping BRD ↔ code

| BRD                                  | Implementation                                                                                  |
|--------------------------------------|-------------------------------------------------------------------------------------------------|
| FR-1 Workflow designer + versioning  | `DefaultWorkflowEngineProvider#publish` bumps version, deactivates previous active definition.  |
| FR-1.3 Approver kinds                | `Enums.ApproverType` + `KeycloakApproverResolver` (user / group / role / `MANAGER_ATTRIBUTE`).   |
| FR-2 SLA + business calendar         | `WorkflowStep.slaMinutes` + `BusinessCalendar.addBusinessMinutes`.                              |
| FR-3 Reminders                       | `WorkflowStep.reminderOffsetsMinutes`, dispatched by `engine.tick()` (idempotent via `firedReminders`). |
| FR-3.3 Channels                      | `MultiChannelNotificationGateway` — Keycloak email templates + JDK HttpClient webhook.          |
| FR-4 Escalation                      | `EscalationType.REASSIGN / AUTO_APPROVE / AUTO_REJECT` applied by `applyEscalation`.            |
| FR-5 Provisioning                    | `KeycloakProvisioningGateway` grants the role/group on full approval.                           |
| FR-6.1 Audit trail                   | Every transition writes a `WorkflowAuditEntity` (`WF_AUDIT`, insert-only).                      |
| FR-6.2 SoD                           | Requester == approver path is blocked at submit (auto-escalate) and at decision time.           |
| NFR-1 Performance                    | `SlaScheduler` runs every 30s as `ClusterAwareScheduledTaskRunner`, separate from auth flow.    |
| NFR-2 Decoupled extension            | Standalone SPI: `WorkflowEngineSpi` + `WorkflowEngineProvider`, plugged via `META-INF/services`. |
| NFR-3 Resilience                     | `JpaWorkflowStore` persists definitions, instances and current deadlines.                       |
| Edge: manager absent                 | Empty resolve → `WorkflowDefinition.fallbackGroupId`.                                            |
| Edge: approver == requester          | SoD escalation in `activateStep`.                                                               |
| Edge: definition edited while running| `WorkflowInstance.definitionVersion` snapshots the version at submit time.                      |

## REST surface (mounted under `/realms/{realm}/workflow`)

| Method | Path                                            | Purpose                            |
|--------|-------------------------------------------------|------------------------------------|
| POST   | `/definitions`                                  | Publish a new workflow version     |
| GET    | `/definitions/active?targetType=&targetId=`     | Fetch the active definition        |
| POST   | `/requests`                                     | Submit an access request           |
| GET    | `/requests/{id}`                                | Read instance state                |
| GET    | `/requests/mine`                                | Caller's submitted requests        |
| GET    | `/requests/inbox`                               | Pending steps assigned to caller   |
| POST   | `/requests/{id}/steps/{stepId}/approve`         | Approve current step               |
| POST   | `/requests/{id}/steps/{stepId}/reject`          | Reject (terminates flow)           |
| POST   | `/requests/{id}/cancel`                         | Requester cancels                  |

## Enterprise enhancements (au-delà du BRD)

| Fonctionnalité                            | Implémentation                                                              |
|-------------------------------------------|-----------------------------------------------------------------------------|
| **Accès JIT / temporaire**                | `WorkflowDefinition.validityMinutes` → `RevocationJob` planifié, révoqué automatiquement par le scheduler à expiration. |
| **Politiques SoD avancées**               | `SodPolicy` (paires de cibles toxiques), vérifiées à la soumission **et** juste avant le provisioning. |
| **Délégation d'approbation**              | `Delegation` (out-of-office) → `applyDelegations` étend l'ensemble des approbateurs éligibles. |
| **Justification métier obligatoire**      | `WorkflowDefinition.requireJustification` + champ `justification` sur l'instance. |
| **Score de risque + étapes conditionnelles** | `riskScoreExpression` (DSL pondéré), `WorkflowStep.skipCondition` (`risk < 30`, `attr.country == FR`...). |
| **Webhooks signés HMAC-SHA256**           | `WorkflowStep.webhookSecret` → header `X-WF-Signature: sha256=...` (Slack/Teams compatible). |
| **Émission d'events Keycloak**            | `KeycloakEventEmitter` publie chaque transition sur l'EventStore (intégration SIEM Splunk/ELK). |
| **Tableau de bord métriques**             | Endpoint `GET /metrics` (counts par statut, breaches SLA 30j, temps moyen d'approbation). |

### Endpoints additionnels

| Méthode | Path                  | Rôle                                           |
|---------|-----------------------|------------------------------------------------|
| GET     | `/delegations`        | Mes délégations actives                        |
| POST    | `/delegations`        | Créer/mettre à jour une délégation             |
| GET     | `/sod-policies`       | Politiques SoD actives                         |
| POST    | `/sod-policies`       | Publier une politique SoD (admin)              |
| GET     | `/metrics`            | Tableau de bord gouvernance                    |

## Extension points

- `WorkflowEngineProvider` — replace the whole engine.
- `WorkflowStore` — alternative persistence (NoSQL, external workflow DB).
- `NotificationGateway` — add Slack/Teams/SMS adapters.
- `ProvisioningGateway` — provision into LDAP/SCIM instead of (or in addition to) Keycloak.
- `ApproverResolver` — custom approver resolution (HRIS lookup, OPA, etc.).

## Tests

`mvn -pl workflow-engine test` runs `EngineFlowTest` which exercises the happy
path, SoD, rejection, and SLA auto-reject escalation using in-memory fakes.
