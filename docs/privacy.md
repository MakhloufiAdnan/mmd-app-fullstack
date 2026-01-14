Conformité : mentions légales & confidentialité (MVP)

Ce projet est un MVP et il n’a pas vocation à être utilisé comme un service commercial en production.

1) Mentions légales (version MVP)
- Auteur : [MAKHLOUFI Adnan]
- Date : [14.01.2026]
- Contexte : projet scolaire / formation
- Hébergement : usage local (en dev). 

2) Données personnelles traitées
Dans l’application, on manipule quelques données utilisateur :
- email (pour identifier l’utilisateur)
- username (affiché dans l’app)
- mot de passe : jamais stocké en clair (stocké hashé côté back)
- contenu généré : posts et commentaires

3) Pourquoi on traite ces données (finalités)
- Authentification : inscription, connexion, maintien de session
- Profil : afficher et modifier les infos de base
- Social : publier et commenter
- Abonnements : filtrer le feed selon les thèmes

4) Cookies / stockage navigateur (important)
L’application utilise des cookies techniques nécessaires :
- refresh token (cookie HttpOnly) : permet de garder l’utilisateur connecté
- XSRF-TOKEN : utilisé pour protéger contre les attaques CSRF
Ces cookies ne servent pas au tracking et ne sont pas des cookies publicitaires.

Côté front :
- l’access token est gardé en mémoire (pas dans le localStorage) pour limiter les risques de vol via XSS.

5) Sécurité (mesures mises en place)
- Mot de passe hashé côté back
- Contrôles de validation côté back (et UX côté front)
- JWT access token + refresh token HttpOnly
- Protection CSRF via cookie XSRF + header
- Logs et erreurs : pas de données sensibles affichées

6) Conservation / suppression (limite du MVP)
Dans ce MVP, il n’y a pas de fonctionnalité “supprimer mon compte”.
