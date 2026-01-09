# MDD — mmd-app-fullstack (Monorepo)

Contrainte : garder un seul repository pour tout le projet. 

MDD est un mini réseau social (MVP) permettant :
- s’inscrire / se connecter (session persistante),
- s’abonner à des thèmes,
- consulter un feed chronologique (tri asc/desc),
- créer des articles et commenter.

> MVP : pas de back-office/admin.

## Structure du repo

- `front/` : application Angular (SPA)
- `back/` : API Spring Boot (REST)
- `docs/` : documentation (choix techniques, FAQ, rapports)

---

## Prérequis
- Node.js + npm
- Java 21
- Angular 20

---

## Démarrage (dev)

### Front
powershell
cd front
npm install
npm start
➡️ App : http://localhost:4200

- Le front utilise un proxy Angular : le navigateur appelle http://localhost:4200/api/* (same-origin),
et le dev server proxyfie vers le back (pas de CORS). 

### Back
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
DB_ROOT_PASSWORD=root_password

# Activer le remplissage de la BD avec des postes et commentaires si vide
SEED_DEMO_DATA=false
```