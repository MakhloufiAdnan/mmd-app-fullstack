# MDD — API Contract (MVP)

## Principes
- Base path: /api
- JSON UTF-8
- Les endpoints marqués 🔒 nécessitent une authentification (Bearer token).
- Les validations sont faites côté back (car le front ne suffit jamais).
- Auteur + date sont définis automatiquement côté back lors de l’ajout d’un article ou commentaire.

## Format d’erreur (proposé, unique)
{
  "error": "VALIDATION_ERROR | UNAUTHORIZED | FORBIDDEN | NOT_FOUND | CONFLICT | INTERNAL",
  "message": "Message lisible",
  "fieldErrors": [
    { "field": "password", "message": "..." }
  ]
}

---

# Auth

## POST /api/auth/register (public)
Request:
{
  "email": "user@mail.com",
  "username": "devUser",
  "password": "P@ssw0rd!"
}

Response 201:
{
  "id": "uuid",
  "email": "user@mail.com",
  "username": "devUser"
}

Errors:
- 400 VALIDATION_ERROR (email/username invalides, password invalide)

## POST /api/auth/login (public)
Request:
{
  "login": "user@mail.com | devUser",
  "password": "P@ssw0rd!"
}

Response 200 :
{
  "accessToken": "jwt-very-strong",
  "tokenType": "Bearer"
}

Errors:
- 401 UNAUTHORIZED (identifiants invalides)

## POST /api/auth/logout 🔒
Response 204
- invalide la session (token)

---

# Utilisateur / Profil

## GET /api/users/me 🔒
Response 200:
{
  "id": "uuid",
  "email": "user@mail.com",
  "username": "devUser"
}

## PUT /api/users/me 🔒
Request (tous les champs optionnels, au moins 1 requis):
{
  "email": "new@mail.com",
  "username": "newUser",
  "password": "NewP@ssw0rd!"
}

Response 200:
{
  "id": "uuid",
  "email": "new@mail.com",
  "username": "newUser"
}

Errors:
- 400 VALIDATION_ERROR (notamment password: min 8 caracteres + chiffre + min + maj + spécial)

---

# Thèmes (Topics)

## GET /api/topics 🔒
Response 200:
[
  { "id": 1, "name": "Java" },
  { "id": 2, "name": "Angular" }
]

---

# Abonnements (Subscriptions)

## GET /api/users/me/subscriptions 🔒
Response 200:
[
  { "topicId": 1, "topicName": "Java" }
]

## POST /api/users/me/subscriptions/{topicId} 🔒
Response 204
Errors:
- 404 NOT_FOUND (topicId inexistant)
- 409 CONFLICT (déjà abonné) [optionnel si tu choisis l’idempotence -> 204]

## DELETE /api/users/me/subscriptions/{topicId} 🔒
Response 204
Errors:
- 404 NOT_FOUND (abonnement inexistant)

---

# Feed

## GET /api/feed 🔒
Query params:
- order=desc|asc (desc par défaut)

Response 200:
[
  {
    "id": 10,
    "topic": { "id": 1, "name": "Java" },
    "title": "Titre",
    "author": { "id": "uuid", "username": "devUser" },
    "createdAt": "2025-12-22T12:00:00Z"
  }
]

---

# Articles (Posts)

## POST /api/posts 🔒
Request:
{
  "topicId": 1,
  "title": "Mon titre",
  "content": "Mon contenu"
}

Response 201:
{
  "id": 10
}

Notes:
- author + createdAt sont définis côté back

## GET /api/posts/{postId} 🔒
Response 200:
{
  "id": 10,
  "topic": { "id": 1, "name": "Java" },
  "title": "Mon titre",
  "content": "Mon contenu",
  "author": { "id": "uuid", "username": "devUser" },
  "createdAt": "2025-12-22T12:00:00Z",
  "comments": [
    {
      "id": 100,
      "content": "Super !",
      "author": { "id": "uuid", "username": "otherUser" },
      "createdAt": "2025-12-22T13:00:00Z"
    }
  ]
}

---

# Commentaires

## POST /api/posts/{postId}/comments 🔒
Request:
{
  "content": "Mon commentaire"
}

Response 201:
{ "id": 100 }

Notes:
- pas de sous-commentaires (non récursif)
- author + createdAt définis côté back
