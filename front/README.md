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

Pour générer une couverture (utile aussi pour Sonar) :
```bash
npx ng test --watch=false --browsers=ChromeHeadless --code-coverage
```

Rapports :

HTML : front/coverage/index.html
LCOV : front/coverage/lcov.info

## SonarQube (mono-repo)

### Commande :
```bash
npm run sonar:local
```

Le script utilisé est ../run-sonar.ps1 (à la racine du repo).

### Token Sonar

Le script lit SONAR_TOKEN :

depuis la variable d’environnement SONAR_TOKEN, sinon

depuis front/.env (gitignored)
```text
Exemple front/.env :

SONAR_TOKEN=__VOTRE_SONAR_TOKEN__
SONAR_HOST_URL=http://localhost:9000
```