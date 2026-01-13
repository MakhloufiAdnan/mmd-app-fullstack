# Front — MDD (Angular)

SPA Angular (standalone) + Angular Material.

En local, l’app consomme l’API via `/api` grâce au proxy (`proxy.conf.json`).

---

## Démarrer (dev)

```bash
npm install
npm start
```
App : http://localhost:4200

Tests
```bash
npm test
```

## Coverage

Générer la couverture
```bash
npm run test:coverage
```

Lire le rapport

Après exécution, Angular génère un rapport de couverture :

HTML : front/coverage/index.html
LCOV : front/coverage/lcov.info (utilisé par Sonar)

➡️ Pour lire le rapport HTML : ouvre front/coverage/index.html dans ton navigateur.

Windows (PowerShell) :
```bash
start front/coverage/index.html
```

macOS :
```bash
open front/coverage/index.html
```

Linux :
```bash
xdg-open front/coverage/index.html
```

Si tu ne vois pas le dossier front/coverage, vérifie que tu as bien lancé --code-coverage (ou npm run test:coverage).

## SonarQube (mono-repo)

Lancer l’analyse
```bash
npm run sonar:local
```

Le script utilisé est ../run-sonar.ps1 (à la racine du repo).

### Points importants

Sonar lit le rapport LCOV généré par le coverage :

- front/coverage/lcov.info

Assure-toi d’avoir exécuté une fois le coverage avant Sonar si tu veux des métriques à jour :
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

E2E (Cypress)

Lancer les tests E2E (headless) :
```bash
npm run e2e:run
```

Ouvrir Cypress (UI) :
```bash
npm run e2e:open
```

Lancer E2E avec auto-start de l’app :
```bash
npm run e2e
```

### Scripts disponibles

npm start : lance l’app en dev + proxy /api

npm test : tests unitaires en headless

npm run test:watch : tests en mode watch

npm run test:coverage : tests + génération coverage

npm run sonar:local : analyse SonarQube (mono-repo)

npm run e2e:run / npm run e2e:open / npm run e2e : Cypress