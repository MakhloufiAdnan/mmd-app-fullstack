/// <reference types="cypress" />

/**
 * Scénario E2E : token expiré → 401 → refresh → retry.
 *
 * Pourquoi "simule" le 401 ?
 * - En E2E, forcer l'expiration réelle d'un JWT dépend du backend (durée, horloge, etc.).
 * - Bonne pratique : piloter cet edge case en stubant un 401 sur un endpoint protégé,
 *   puis vérifier le comportement du client : refresh puis retry.
 *
 * Ce test cible le contrat de l'interceptor :
 * - 401 sur GET /api/feed  → POST /api/auth/refresh → retry GET /api/feed avec nouveau Bearer
 *
 * Note : l'app lance un bootstrap (csrf -> refresh) au démarrage.
 * stub le 1er refresh (bootstrap) en 401 pour éviter une auth implicite avant login,
 * stub le 2e refresh (celui déclenché par 401) en succès.
 */
describe('Auth - Refresh on 401 then retry', () => {
  before(() => {
    // Arrange: garantit que l'utilisateur e2e existe (idempotent)
    cy.ensureE2EUserExists();
  });

  it('401 on /api/feed should trigger refresh and retry with new token', () => {
    // Arrange: credentials
    const identifier = Cypress.env('e2eUsername') as string;
    const password = Cypress.env('e2ePassword') as string;

    // Arrange: stub /api/auth/refresh
    // - 1er appel = bootstrap -> 401
    // - 2e appel = après 401 -> 200 avec token déterministe
    let refreshCalls = 0;
    cy.intercept('POST', '**/api/auth/refresh', (req) => {
      refreshCalls += 1;

      if (refreshCalls === 1) {
        req.reply({
          statusCode: 401,
          body: { error: 'UNAUTHORIZED', message: 'No refresh cookie (bootstrap)' },
        });
        return;
      }

      req.reply({
        statusCode: 200,
        body: {
          accessToken: 'new-jwt',
          tokenType: 'Bearer',
          expiresInSeconds: 900,
        },
      });
    }).as('refresh');

    // Arrange: stub /api/feed
    // - 1er appel -> 401 (simulate "token expiré")
    // - 2e appel -> 200 et on vérifie le header Authorization du retry
    const createdAt = new Date().toISOString();
    let feedCalls = 0;

    cy.intercept('GET', '**/api/feed*', (req) => {
      feedCalls += 1;

      if (feedCalls === 1) {
        req.reply({
          statusCode: 401,
          body: { error: 'UNAUTHORIZED', message: 'Expired access token' },
        });
        return;
      }

      // Assert (dans l'intercept) : le retry porte bien le token rafraîchi
      expect(req.headers).to.have.property('authorization', 'Bearer new-jwt');

      req.reply({
        statusCode: 200,
        body: [
          {
            id: 999999,
            topic: { id: 1, name: 'E2E Topic' },
            title: 'Post after retry',
            content: 'This feed item is returned after refresh+retry.',
            author: { id: 1, username: identifier },
            createdAt,
            commentsCount: 0,
          },
        ],
      });
    }).as('feed');

    // Arrange: spy login (UI)
    cy.intercept('POST', '**/api/auth/login').as('login');

    // Act: ouvre /login (déclenche le bootstrap refresh)
    cy.visit('/login');

    // Assert: on consomme l'appel refresh de bootstrap pour ne pas confondre avec celui du 401.
    cy.wait('@refresh').its('response.statusCode').should('eq', 401);

    // Act: login via UI
    cy.get('[data-testid="login-identifier"]').clear().type(identifier);
    cy.get('[data-testid="login-password"]').clear().type(password, { log: false });
    cy.get('[data-testid="login-submit"]').click();

    // Assert: login OK, redirection /feed
    cy.wait('@login').its('response.statusCode').should('be.oneOf', [200, 201, 204]);
    cy.location('pathname', { timeout: 10_000 }).should('eq', '/feed');

    // Assert: 1er feed = 401 (déclenche refresh)
    cy.wait('@feed').its('response.statusCode').should('eq', 401);

    // Assert: refresh déclenché par le 401 (2e call) renvoie le token attendu
    cy.wait('@refresh').then((i) => {
      expect(i.response?.statusCode).to.eq(200);
      expect(i.response?.body).to.have.property('accessToken', 'new-jwt');
    });

    // Assert: retry feed = 200
    cy.wait('@feed').its('response.statusCode').should('eq', 200);

    // Assert: UI rendu (titre de l'item stub)
    cy.contains('.feed-title', 'Post after retry', { timeout: 10_000 }).should('be.visible');

    // Assert: pas de boucle infinie (comptage attendu)
    cy.wrap(null).then(() => {
      expect(refreshCalls, 'refresh call count').to.eq(2);
      expect(feedCalls, 'feed call count').to.eq(2);
    });
  });
});
