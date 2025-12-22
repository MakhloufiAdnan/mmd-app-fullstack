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

### 2.3 CSRF (obligatoire dès qu’on utilise des cookies d’auth)

- Cookie CSRF (lisible par Angular) : `XSRF-TOKEN` (non HttpOnly)
- Header envoyé par Angular : `X-XSRF-TOKEN: <valeur du cookie>`
- **Exigé** au minimum sur :
  - `POST /api/auth/refresh`
  - `POST /api/auth/logout`
- Sur tous les POST/PUT/DELETE, même si Bearer header.

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
```

---

## 4) Auth

### 4.1 GET /api/auth/csrf (public)

But : initialiser le cookie CSRF pour les SPA.

**Request** : vide  
**Response 204** : no content  
**Headers** (exemple) :

- `Set-Cookie: XSRF-TOKEN=<token>; Path=/; SameSite=Lax`

> Remarque : avec Spring Security, le cookie CSRF peut aussi être émis automatiquement sur d’autres réponses.
> Ce endpoint permet simplement d’avoir un point d’entrée clair côté front.

---

### 4.2 POST /api/auth/register (public)

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

- 400 VALIDATION_ERROR (password policy, email invalide, champs manquants)
- 409 CONFLICT (email/username déjà utilisé)

---

### 4.3 POST /api/auth/login (public)

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

- `Set-Cookie: refreshToken=<opaque-or-jwt>; HttpOnly; Secure; SameSite=Lax; Path=/api/auth`
- (optionnel) `Set-Cookie: XSRF-TOKEN=<token>; Path=/; SameSite=Lax`

Notes:

- L’**access token** est utilisé dans `Authorization`.
- Le **refresh token** n’est jamais exposé au JavaScript (cookie HttpOnly).

---

### 4.4 POST /api/auth/refresh (public, cookie requis + CSRF)

But : obtenir un nouvel access token si le refresh token cookie est valide.

Pré-requis :

- Cookie `refreshToken` présent
- Header `X-XSRF-TOKEN` présent (valeur du cookie `XSRF-TOKEN`)

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

Headers (rotation recommandée) :

- `Set-Cookie: refreshToken=<new>; HttpOnly; Secure; SameSite=Lax; Path=/api/auth`
- (optionnel) `Set-Cookie: XSRF-TOKEN=<token>; Path=/; SameSite=Lax`

Erreurs:

- 401 UNAUTHORIZED (refresh expiré/invalide)
- 403 FORBIDDEN (CSRF manquant/invalide)

---

### 4.5 POST /api/auth/logout 🔒 (cookie requis + CSRF)

But : invalider la session persistante (refresh) et déconnecter.

Pré-requis :

- Cookie `refreshToken` présent
- Header `X-XSRF-TOKEN` présent

Request:

```json
{}
```

Response 204 (no content)

Headers (exemple) :

- `Set-Cookie: refreshToken=; Max-Age=0; Path=/api/auth; HttpOnly; Secure; SameSite=Lax`

Notes:

- Côté front, on supprime aussi l’access token en mémoire.

---

## 5) Profil

### 5.1 GET /api/users/me 🔒

Response 200:

```json
{
  "id": 1,
  "email": "user@mail.com",
  "username": "devUser",
  "subscriptions": [{ "id": 10, "name": "Java" }]
}
```

---

### 5.2 PUT /api/users/me 🔒

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
```

Notes:

- Tous les champs peuvent être optionnels (PATCH-like) **ou** imposés (PUT strict) : à choisir à l’implémentation.
- Le back revalide toujours la politique mdp.

---

## 6) Topics

### 6.1 GET /api/topics 🔒

Response 200:

```json
[
  { "id": 1, "name": "Java", "subscribed": true },
  { "id": 2, "name": "Angular", "subscribed": false }
]
```

---

## 7) Subscriptions

### 7.1 POST /api/users/me/subscriptions 🔒

Request:

```json
{ "topicId": 11 }
```

Response 201:

```json
{ "id": 11 }
```

Erreurs:

- 409 CONFLICT (déjà abonné)

---

### 7.2 DELETE /api/users/me/subscriptions/{topicId} 🔒

Response 204

---

## 8) Feed (articles)

### 8.1 GET /api/feed 🔒

Query params:

- `order=desc|asc` (default `desc`)
- (option MVP) `topicId=<id>`

Response 200:

```json
[
  {
    "id": 100,
    "topic": { "id": 10, "name": "Java" },
    "title": "Titre",
    "author": { "id": 1, "username": "devUser" },
    "createdAt": "2025-12-22T12:00:00Z",
    "commentsCount": 2
  }
]
```

---

## 9) Posts

### 9.1 POST /api/posts 🔒

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

- `author` + `createdAt` définis côté back.

---

### 9.2 GET /api/posts/{postId} 🔒

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

---

## 10) Commentaires

### 10.1 POST /api/posts/{postId}/comments 🔒

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

- pas de sous-commentaires (non récursif)
- author + createdAt définis côté back

---

## 11) Codes HTTP (rappel)

- 200 OK (lecture / update)
- 201 Created (création)
- 204 No Content (delete/logout/csrf)
- 400 Validation
- 401 Unauthorized (non authentifié / token expiré)
- 403 Forbidden (pas le droit / CSRF invalide)
- 404 Not Found
- 409 Conflict
- 500 Internal

---

## 12) Flux SPA (résumé)

1. Au chargement de l’app : `GET /api/auth/csrf` puis `POST /api/auth/refresh`
2. Si refresh OK : stocker access token **en mémoire** + naviguer sur routes protégées
3. Interceptor : si un appel 🔒 répond 401, tenter **une seule fois** `refresh` puis rejouer la requête
4. Logout : `POST /api/auth/logout` (CSRF) puis purge access token en mémoire
