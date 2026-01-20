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
  - affichage chronologique des posts liés aux thèmes abonnés (**tri asc/desc**)
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

- Front : Angular (standalone) + Angular Material
- Back : Java 21 + Spring Boot 3.x
- DB : MySQL (Docker)
- Tests : Front (Karma/Jasmine), Back (JUnit 5 / MockMvc / Testcontainers)
- Coverage : Istanbul (front), JaCoCo (back)

> Détails d’installation, scripts, tests et coverage :
>
> - Front : `front/README.md`
> - Back : `back/README.md`

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

### Arborescence

```text
mmd-app-fullstack/
├── front/
├── docs/
└── back/
```

0. Cloner le projet
     git clone https://github.com/MakhloufiAdnan/mmd-app-fullstack.git
     cd mmd-app-fullstack

1. Démarrer la DB (MySQL)
   Créer back/.env (non versionné), puis :

```bash
cd back
docker compose up -d
```
2. Lancer le back
```bash
./mvnw spring-boot:run
cd..
```
3. Lancer le front
```bash
cd front
npm install
npm start
```
## SonarQube (mono-repo)

L’analyse Sonar se lance depuis le front :
```bash
npm run sonar:local
```
Le script run-sonar.ps1 est à la racine du repo.

## Décisions & écarts par rapport aux specs

Abonnement requis

- Créer un post : l’utilisateur doit être abonné au topic choisi
- Commenter : l’utilisateur doit être abonné au topic du post

Ces règles ne sont pas explicitement écrites dans les specs MVP, mais elles sont cohérentes avec le feed (topics abonnés) et évitent des contenus hors-sujet.

CSRF

CSRF utilisé principalement pour les flux basés cookie (ex : refresh/logout).
Les appels protégés en Bearer ne nécessitent pas forcément CSRF (comportement validé par tests).

CORS (dev)

Le front utilise un proxy Angular :

- le navigateur appelle http://localhost:4200/api/\* (same-origin),
- le dev server proxyfie vers le back → pas de CORS requis en local.

## Documentation
- Justification des choix techniques (PDF) : docs/justification-choix-techniques.pdf
- Contrat API : docs/api-contract.md
- Conformité (privacy / cookies) : docs/privacy.md