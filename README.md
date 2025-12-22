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

``` text
.env.example
DB_NAME=mdd
DB_USER=mdd
DB_PASSWORD=mdd
DB_ROOT_PASSWORD=root_password

# Back en local + MySQL en docker avec port 3307
DB_HOST=localhost
DB_PORT=3307

JWT_EXPIRATION_MS=900000
REFRESH_TOKEN_EXPIRATION_MS=1209600000
TOKEN_SECRET=dev-secret-change-me
```