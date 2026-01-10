# MDD — mmd-app-fullstack (Monorepo)

MDD est un mini réseau social (MVP) permettant :
- s’inscrire / se connecter (**session persistante** via refresh token HttpOnly),
- s’abonner à des thèmes,
- consulter un feed chronologique (**tri asc/desc**),
- créer des articles et commenter.

> MVP : pas de back-office/admin.  
> Contrainte : mono-repo (un seul repository pour front + back + docs).

---

## Structure du repo

- `front/` : application Angular (SPA)
- `back/` : API Spring Boot (REST)
- `docs/` : documentation (choix techniques, FAQ, rapports et contrat API)

L’application consomme l’API via **`/api`** (proxy Angular → back en local).

---

## Fonctionnalités MVP

- Auth :
  - inscription / connexion
  - refresh token (cookie HttpOnly)
  - logout
  - endpoint CSRF
- Topics :
  - lister les thèmes
  - s’abonner / se désabonner
- Feed :
  - affichage chronologique des posts liés aux thèmes
- Posts :
  - créer un post
  - consulter le détail d’un post
- Comments :
  - ajouter un commentaire sur un post
- Profil :
  - consulter “Me”
  - mettre à jour email / username / mot de passe

---

## Stack & versions

### Front
- Angular (standalone)
- Angular Material
- Tests : Karma + Jasmine (+ Istanbul coverage)

### Back
- Java 21
- Spring Boot 3.x
- MySQL (Docker)
- Tests : JUnit 5 + Spring Boot Test + MockMvc + Testcontainers
- Couverture : JaCoCo

---

## Ports & URLs

- Front : `http://localhost:4200`
- Back : `http://localhost:8080`
- API : `http://localhost:8080/api`

---

## Pré-requis

- Node.js (LTS recommandé) + npm
- Java 21
- Docker (obligatoire : MySQL dev + Testcontainers)
- Git

---

## Quickstart (local)

### Arborescence :
```text
mmd-app-fullstack/
├── front/
├── docs/
└── back/
```

1) Démarrer la DB (Docker)
Dans back/, créer un fichier .env à partir de .env.example 
```
cd back
cp .env.example .env
```
Exemple .env.example (dev uniquement)
```
DB_NAME=mdd
DB_USER=mdd
DB_PASSWORD=mdd
DB_ROOT_PASSWORD=root_password

// Back en local + MySQL en docker avec port 3307
DB_HOST=localhost
DB_PORT=3307

JWT_EXPIRATION_MS=900000
REFRESH_TOKEN_EXPIRATION_MS=1209600000
TOKEN_SECRET=dev-secret-change-me

// Activer le remplissage de la BD avec des posts/comments si vide
SEED_DEMO_DATA=false
```

⚠️ Ne pas committer .env (secrets). Garder uniquement .env.example.

### Démarrer MySQL :
docker compose up -d

2) Lancer le back
cd back
./mvnw spring-boot:run

3) Lancer le front
cd front
npm install
npm start
Ouvrir : http://localhost:4200

## Tests & couverture

### Back
cd back
./mvnw test

### Rapport JaCoCo :
back/target/site/jacoco/index.html

### Front
cd front
ng test --code-coverage

### Rapport coverage (Angular) :
front/coverage/**/index.html

### Décisions & écarts par rapport aux specs

* Abonnement requis (règle ajoutée côté back) :

> Créer un post : l’utilisateur doit être abonné au topic choisi
> Commenter : l’utilisateur doit être abonné au topic du post

> Ces règles ne sont pas explicitement écrites dans les specs MVP, mais elles sont cohérentes avec le feed (topics abonnés) et évite des contenus hors-sujet.

* CSRF
CSRF utilisé principalement pour les flux basés cookie (ex: refresh/logout).
Les appels protégés en Bearer ne nécessitent pas forcément CSRF (comportement validé par tests).

* CORS (dev)
Le front utilise un proxy Angular :
> le navigateur appelle http://localhost:4200/api/* (same-origin),

> le dev server proxyfie vers le back → pas de CORS requis en local.