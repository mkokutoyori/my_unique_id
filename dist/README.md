# Workflow Engine — build artifact

Build: `keycloak-workflow-engine-999.0.0-SNAPSHOT` (branche `claude/festive-volta-5tMUt`).

| Fichier | Taille | SHA-256 |
|---|---|---|
| `keycloak-workflow-engine.jar` | ~95 KB | `8581001f7050ee9fe2c34217601a97fe0099e41c334c489ad9d1d2b6c5c8a9d2` |

## Téléchargement

URL brute (raw) :

```
https://github.com/mkokutoyori/my_unique_id/raw/claude/festive-volta-5tMUt/dist/keycloak-workflow-engine.jar
```

## Déploiement sur une distribution Keycloak existante

1. Copier le JAR dans le répertoire des providers de Keycloak :
   ```bash
   cp keycloak-workflow-engine.jar $KEYCLOAK_HOME/providers/
   ```
2. Reconstruire Keycloak pour qu'il intègre l'extension :
   ```bash
   $KEYCLOAK_HOME/bin/kc.sh build
   ```
3. Démarrer Keycloak normalement :
   ```bash
   $KEYCLOAK_HOME/bin/kc.sh start
   ```
4. Au premier appel à `/realms/{realm}/workflow/ui`, le client OIDC `workflow-console` est créé automatiquement dans le realm et la console est servie en SSO.

Les tables `WF_DEFINITION`, `WF_INSTANCE`, `WF_AUDIT`, `WF_DELEGATION`,
`WF_SOD_POLICY` et `WF_REVOCATION` sont créées via Liquibase au démarrage.
