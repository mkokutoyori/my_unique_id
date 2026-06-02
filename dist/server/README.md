# my_unique_id — distribution complète

Distribution Keycloak 26.0.7 avec le module **workflow-engine** pré-installé
dans `providers/`. Le tarball étant > 100 Mo (limite GitHub par fichier), il
est livré en 2 morceaux.

| Fichier | Taille |
|---|---|
| `my_unique_id-26.0.7.tar.gz.part-aa` | 70 Mo |
| `my_unique_id-26.0.7.tar.gz.part-ab` | 70 Mo |
| `my_unique_id-26.0.7.tar.gz` (reconstitué) | 140 Mo |
| SHA-256 (reconstitué) | `1fce51a8b8d9ca5e72ecae541203fba95ad4c0f0ac27e8e88ffe450c1bac9c61` |

## Téléchargement + reconstruction

```bash
BASE=https://github.com/mkokutoyori/my_unique_id/raw/claude/festive-volta-5tMUt/dist/server
curl -sSL -o aa $BASE/my_unique_id-26.0.7.tar.gz.part-aa
curl -sSL -o ab $BASE/my_unique_id-26.0.7.tar.gz.part-ab
cat aa ab > my_unique_id-26.0.7.tar.gz
sha256sum my_unique_id-26.0.7.tar.gz   # attendu: 1fce51a8b8d9ca5e72ecae541203fba95ad4c0f0ac27e8e88ffe450c1bac9c61
tar xzf my_unique_id-26.0.7.tar.gz
cd my_unique_id-26.0.7
```

## Démarrage

```bash
# Premier lancement : crée la base H2 locale et l'admin user
bin/kc.sh start-dev --bootstrap-admin-username=admin --bootstrap-admin-password=admin
```

Une fois lancé, ouvrez :

- Admin console : http://localhost:8080/admin/
- **Workflow Engine console** : http://localhost:8080/realms/master/workflow/ui

Au premier accès à `/workflow/ui`, le client OIDC `workflow-console` est
auto-provisionné dans le realm courant et les tables `WF_*` sont créées par
Liquibase.

## Important

Cette distribution intègre **Keycloak 26.0.7 amont + workflow-engine.jar**.
Le code Java amont n'est pas modifié — la valeur ajoutée de votre fork
réside intégralement dans `providers/keycloak-workflow-engine.jar`.

Pour produire une distribution 100 % construite depuis vos sources
`999.0.0-SNAPSHOT`, il faut lancer le build localement (réseau ouvert
pour la génération OpenAPI du JS admin client) :

```bash
./mvnw -pl quarkus/dist -am -DskipTests -DskipTestsuite -DskipExamples package
ls quarkus/dist/target/*.tar.gz
```
