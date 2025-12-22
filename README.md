# MDD — mmd-app-fullstack (Monorepo)

Contrainte : garder un seul repository pour tout le projet. :contentReference[oaicite:1]{index=1}

## Structure
- /front : Angular (UI)
- /back  : Spring Boot (API)
- /docs  : documentation (contrat API, etc.)

## Prérequis
- Node.js + npm
- Java 21
- Angular 20

## Démarrage (dev)

### Front
```powershell
cd front
npm install
npm start
➡️ App : http://localhost:4200

Back
powershell
Copier le code
cd back
./mvnw spring-boot:run
➡️ API : http://localhost:8080