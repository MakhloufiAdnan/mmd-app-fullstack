# Back — MDD API (Spring Boot)

API REST Spring Boot (Java 21) + MySQL (Docker).

---

## Configuration (`back/.env`)

Créer un fichier `back/.env` (non versionné) au niveau de `back/` (format `key=value`).

Exemple minimal (dev) :

```properties
DB_NAME=mdd
DB_USER=mdd
DB_PASSWORD=mdd
DB_ROOT_PASSWORD=root_password

DB_HOST=localhost
DB_PORT=3307

JWT_EXPIRATION_MS=900000
REFRESH_TOKEN_EXPIRATION_MS=1209600000
TOKEN_SECRET=dev-secret-change-me

SEED_DEMO_DATA=false
```

## Démarrer MySQL (Docker)

⚠️ À exécuter depuis back/ (Docker Compose lit le .env du dossier courant) :

```bash
cd back
docker compose up -d
```

MySQL : localhost:3307 → container 3306.

## Lancer l’API
```bash
./mvnw spring-boot:run
```
API : http://localhost:8080

## Tests & coverage (JaCoCo)
```bash
./mvnw test
```

Rapports :

HTML : back/target/site/jacoco/index.html
XML : back/target/site/jacoco/jacoco.xml