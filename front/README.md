# Front — MDD (Angular)

SPA Angular (standalone) + Angular Material.

En local, l’app consomme l’API via `/api` grâce au proxy (`proxy.conf.json`).

En **mode prod-like (pour Lighthouse)**, l’app est servie depuis le build `dist/` via `server.cjs` (serve statique + reverse proxy `/api` vers le back), sur le **port 4300**.

## Prérequis

- Node.js + npm
- API back disponible (ou proxy configuré pour pointer vers l’API)

---

## Démarrer (dev)

Depuis le dossier `front/` :

```bash
npm install
npm start
```
App : http://localhost:4200

Tests
```bash
npm test
```

Mode watch :
```bash
npm run test:watch
```

## Démarrer (prod-like / Lighthouse)

Objectif : mesurer Lighthouse sur un build production (minify + tree-shaking) servi en statique, pas via ng serve.

### Depuis le dossier front/ :
```bash
npm run prod
```
App : http://localhost:4300

## Coverage

Générer la couverture
```bash
npm run test:coverage
```
### Lire les rapports
- Rapports générés (depuis le dossier front/) :

HTML : coverage/index.html
LCOV : coverage/lcov.info (utilisé par Sonar)

- Ouvrir le rapport HTML :

Windows (PowerShell) — depuis front/ :
```bash
start .\coverage\index.html
```
macOS :
```bash
open ./coverage/index.html
```

Linux :
```bash
xdg-open ./coverage/index.html
```

Si la commande est exécutée depuis la racine du mono-repo, le chemin devient :
```bash
front/coverage/index.html.
```

- Coverage des tests E2E (Cypress)

Lancer les E2E avec coverage (instrumentation + rapport) :

```bash
npm run e2e:coverage
```

- Rapports générés (depuis le dossier front/) :

HTML : coverage/cypress/index.html
LCOV : coverage/cypress/lcov.info

- Ouvrir le rapport HTML E2E :

Windows (PowerShell) — depuis front/ :
```bash
start .\coverage\cypress\index.html
```

- E2E (Cypress)
Ouvrir Cypress (UI) :
```bash
npm run cypress:open
```

- Lancer Cypress en headless :
```bash
npm run cypress:run
```

- Lancer E2E avec auto-start de l’app (dev) :
```bash
npm run e2e
```

e2e démarre l’app via npm start puis exécute cypress run.

## SonarQube (mono-repo)

Lancer l’analyse :
```bash
npm run sonar:local
```

Le script utilisé est ../run-sonar.ps1 (à la racine du repo).

### Points importants

Sonar lit le rapport LCOV des tests unitaires :

front/coverage/lcov.info

Recommandé avant Sonar :

```bash
npm run test:coverage
npm run sonar:local
```

## Token Sonar

Le script lit SONAR_TOKEN :

depuis la variable d’environnement SONAR_TOKEN, sinon

depuis front/.env (gitignored)

Exemple front/.env :
```test
SONAR_TOKEN=__VOTRE_SONAR_TOKEN__
SONAR_HOST_URL=http://localhost:9000
```
### Scripts disponibles

npm start : lance l’app en dev + proxy /api

npm run build : build Angular

npm test : tests unitaires headless

npm run test:watch : tests en mode watch

npm run test:coverage : tests + coverage (HTML + lcov)

npm run sonar:local : analyse SonarQube (mono-repo)

npm run cypress:open : Cypress UI

npm run cypress:run : Cypress headless

npm run e2e : start app + Cypress headless

npm run e2e:coverage : start app instrumentée + Cypress + rapport coverage E2E

npm run prod : build prod + serveur statique + proxy /api (port 4300, via server.cjs)