# MDD — API Contract (MVP)

## 1) Principes

- **Base path** : `/api`
- **Format** : JSON UTF‑8 (`Content-Type: application/json`)
- **Auth** :
  - Endpoints marqués 🔒 : **Authorization: Bearer <accessToken>**
  - Persistance : **refresh token** stocké en **cookie HttpOnly** (renouvellement via `/api/auth/refresh`)
- **Validation back** : toutes les validations sont faites côté back (le front ne suffit jamais).
- **Auteur + date** : définis automatiquement côté back lors de la création d’un article ou commentaire.

---

## 2) Cookies & en-têtes (contrat)

### 2.1 Authorization header (access token)

- `Authorization: Bearer <accessToken>`

### 2.2 Cookie refresh token (persistant)

- Nom : `refreshToken`
- Attributs recommandés (prod) :
  - `HttpOnly; Secure; SameSite=Lax; Path=/api/auth`
- Notes :
  - En **dev** (HTTP), `Secure` peut être désactivé.
  - En **prod**, servir front+back sur le **même site** (ou proxy) pour éviter les complexités CORS + cookies cross-site.

### 2.3 CSRF (global, activé)

- Cookie CSRF (lisible par Angular) : `XSRF-TOKEN` (non HttpOnly)
- Header envoyé par le client : `X-XSRF-TOKEN: <valeur du cookie>`
- **Obligatoire sur toutes les requêtes mutantes** : `POST`, `PUT`, `PATCH`, `DELETE`
  - y compris celles avec `Authorization: Bearer …`
- Si CSRF manquant/invalide : **403 FORBIDDEN**

---

## 3) Format d’erreur (unique)

```json
{
  "error": "VALIDATION_ERROR | UNAUTHORIZED | FORBIDDEN | NOT_FOUND | CONFLICT | INTERNAL",
  "message": "Message lisible",
  "fieldErrors": [
    { "field": "password", "message": "..." }
  ]
}
4) Auth
4.1 GET /api/auth/csrf (public)
But : initialiser le cookie CSRF pour les SPA.

Request : vide
Response 204 : no content
Headers (exemple) :

Set-Cookie: XSRF-TOKEN=<token>; Path=/; SameSite=Lax

Remarque : le cookie CSRF peut aussi être émis sur d’autres réponses.
Ce endpoint donne un point d’entrée clair côté front.

4.2 POST /api/auth/register (public + CSRF requis)
Request:

```json
{
  "email": "user@mail.com",
  "username": "devUser",
  "password": "P@ssw0rd!"
}
```

Response 201:

```json
{ "id": 1 }
```

Erreurs:

400 VALIDATION_ERROR (email invalide, champs manquants, password policy)

403 FORBIDDEN (CSRF manquant/invalide)

409 CONFLICT (email/username déjà utilisé)

Password policy (back) : >= 8 et contient minuscule + majuscule + chiffre + spécial.

4.3 POST /api/auth/login (public + CSRF requis)
Request:

```json
{
  "identifier": "user@mail.com",
  "password": "P@ssw0rd!"
}
```

`identifier` = email **ou** username.

Response 200:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

Headers (exemple) :

Set-Cookie: refreshToken=<token>; HttpOnly; Secure; SameSite=Lax; Path=/api/auth

Erreurs:

400 VALIDATION_ERROR (champs manquants / format)

401 UNAUTHORIZED (identifiants invalides)

403 FORBIDDEN (CSRF manquant/invalide)

4.4 POST /api/auth/refresh (public + cookie requis + CSRF requis)
But : obtenir un nouvel access token si le refresh token cookie est valide.

Pré-requis :

Cookie refreshToken présent

Header X-XSRF-TOKEN présent

Request:

```json
{}
```

Response 200:

```json
{
  "accessToken": "<new-jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

Headers (rotation) :

Set-Cookie: refreshToken=<new>; HttpOnly; Secure; SameSite=Lax; Path=/api/auth

Erreurs:

401 UNAUTHORIZED (refresh expiré/invalide/manquant)

403 FORBIDDEN (CSRF manquant/invalide)

Note implémentation : un Authorization expiré sur cet endpoint ne doit pas bloquer le refresh.

4.5 POST /api/auth/logout 🔒 (cookie requis + CSRF requis)
But : invalider la session persistante (refresh) et déconnecter.

Pré-requis :

Authorization: Bearer <accessToken> (endpoint 🔒)

Cookie refreshToken présent

Header X-XSRF-TOKEN présent

Request:

```json
{}
```

Response 204 (no content)

Headers (exemple) :

Set-Cookie: refreshToken=; Max-Age=0; Path=/api/auth; HttpOnly; Secure; SameSite=Lax

Erreurs:

401 UNAUTHORIZED (non authentifié ou refresh manquant/invalide)

403 FORBIDDEN (CSRF manquant/invalide)

5) Profil
5.1 GET /api/users/me 🔒
Response 200:

```json
{
  "id": 1,
  "email": "user@mail.com",
  "username": "devUser",
  "subscriptions": [{ "id": 10, "name": "Java" }]
}
```

401 UNAUTHORIZED

5.2 PUT /api/users/me 🔒 (PATCH-like + CSRF requis)
Champs optionnels. Les champs absents ne sont pas modifiés.

Request:

```json
{
  "email": "new@mail.com",
  "username": "newUser",
  "password": "NewP@ssw0rd!"
}
```

Response 200:

```json
{ "updated": true }
Notes:

Si password est fourni : pas d’ancien mot de passe requis (décision MVP / implémentation actuelle).

Le back revalide toujours la policy mdp.

Erreurs:

400 VALIDATION_ERROR (format email, password policy, etc.)

401 UNAUTHORIZED

403 FORBIDDEN (CSRF)

409 CONFLICT (email/username déjà utilisé)

6) Topics
6.1 GET /api/topics 🔒
Response 200:

```json
[
  {
    "id": 1,
    "name": "Java",
    "description": "Java est un langage de programmation",
    "subscribed": true
  },
  {
    "id": 2,
    "name": "Angular",
    "description": "Angular est un framework frontend",
    "subscribed": false
  }
]
```

Erreurs:

401 UNAUTHORIZED

7) Subscriptions
7.1 POST /api/users/me/subscriptions 🔒 + CSRF requis
Request:

```json
{ "topicId": 11 }
```

Response 201:

```json
{ "id": 11 }
```

id = topicId (identifiant du topic abonné), pas l’id technique de la subscription.

Erreurs:

400 VALIDATION_ERROR

401 UNAUTHORIZED

403 FORBIDDEN (CSRF)

404 NOT_FOUND (topic inconnu)

409 CONFLICT (déjà abonné)

7.2 DELETE /api/users/me/subscriptions/{topicId} 🔒 + CSRF requis
Response 204 (no content)

Idempotent : renvoie 204 même si l’abonnement n’existe pas.

Erreurs:

401 UNAUTHORIZED

403 FORBIDDEN (CSRF)

8) Feed (articles)
8.1 GET /api/feed 🔒
Query params:

order=desc|asc (default desc)

topicId=<id> (optionnel : filtre sur un topic)

Response 200:

```json
[
  {
    "id": 100,
    "topic": { "id": 10, "name": "Java" },
    "title": "Titre",
    "content": "Contenu complet…",
    "author": { "id": 1, "username": "devUser" },
    "createdAt": "2025-12-22T12:00:00Z",
    "commentsCount": 2
  }
]
```

Erreurs:

401 UNAUTHORIZED

9) Posts
9.1 POST /api/posts 🔒 + CSRF requis
Request:

```json
{
  "topicId": 1,
  "title": "Mon titre",
  "content": "Mon contenu"
}
```

Response 201:

```json
{ "id": 10 }
```

Notes:

author + createdAt définis côté back.

Règle implémentée : si non abonné au topic → 403.

Erreurs:

400 VALIDATION_ERROR

401 UNAUTHORIZED

403 FORBIDDEN (CSRF manquant/invalide ou non abonné au topic)

404 NOT_FOUND (topicId inconnu)

9.2 GET /api/posts/{postId} 🔒
Response 200:

```json
{
  "id": 10,
  "topic": { "id": 1, "name": "Java" },
  "title": "Mon titre",
  "content": "Mon contenu",
  "author": { "id": 1, "username": "devUser" },
  "createdAt": "2025-12-22T12:00:00Z",
  "comments": [
    {
      "id": 200,
      "content": "Super !",
      "author": { "id": 2, "username": "otherUser" },
      "createdAt": "2025-12-22T13:00:00Z"
    }
  ]
}
```

Erreurs:

401 UNAUTHORIZED

404 NOT_FOUND (postId inconnu)

10) Commentaires
10.1 POST /api/posts/{postId}/comments 🔒 + CSRF requis
Request:

```json
{
  "content": "Mon commentaire"
}
```

Response 201:

```json
{ "id": 200 }
```

Notes:

pas de sous-commentaires (non récursif)

author + createdAt définis côté back

Règle implémentée : si non abonné au topic du post → 403

Erreurs:

400 VALIDATION_ERROR

401 UNAUTHORIZED

403 FORBIDDEN (CSRF manquant/invalide ou non abonné au topic)

404 NOT_FOUND (postId inconnu)

11) Codes HTTP (rappel)
200 OK (lecture / update)

201 Created (création)

204 No Content (delete/csrf/logout)

400 Validation

401 Unauthorized (non authentifié / token expiré / refresh manquant)

403 Forbidden (pas le droit / CSRF invalide / non abonné)

404 Not Found

409 Conflict

500 Internal

12) Flux SPA (résumé)
Au chargement de l’app : GET /api/auth/csrf (init cookie CSRF) puis POST /api/auth/refresh

Si refresh OK : stocker access token en mémoire + naviguer sur routes protégées

Interceptor : si un appel 🔒 répond 401, tenter une seule fois refresh puis rejouer la requête

Logout : POST /api/auth/logout (CSRF + cookie) puis purge access token en mémoire