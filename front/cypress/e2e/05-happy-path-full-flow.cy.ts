/// <reference types="cypress" />

/**
 * Scénario E2E complet (happy path) :
 * login → feed → subscribe topic → create post → create comment → logout
 *
 * Objectif : valider la chaîne "utilisateur" la plus critique sans sur-tester l'UI.
 * - Sélecteurs : data-testid (quand disponible) pour réduire la fragilité.
 * - Données : uniques (timestamp) pour éviter les collisions (tests idempotents).
 */
describe('E2E - Happy path complet', () => {
  before(() => {
    // Arrange: garantit que l'utilisateur de test existe (OK si déjà créé)
    cy.ensureE2EUserExists();
  });

  it('login → feed → subscribe → create post → comment → logout', () => {
    // Arrange: données uniques pour ce run
    const ts = Date.now();
    const title = `Post E2E ${ts}`;
    const content = `Contenu E2E ${ts}`;
    const comment = `Commentaire E2E ${ts}`;

    const identifier = Cypress.env('e2eUsername') as string;
    const password = Cypress.env('e2ePassword') as string;

    // --- LOGIN (UI) ---
    // Arrange: visite la page de login
    cy.visit('/login');
    cy.intercept('POST', '**/api/auth/login').as('login');

    // Act: saisie identifiants + submit
    cy.get('[data-testid="login-identifier"]').clear().type(identifier);
    cy.get('[data-testid="login-password"]').clear().type(password, { log: false });
    cy.get('[data-testid="login-submit"]').click();

    // Assert: login OK + redirection
    cy.wait('@login').its('response.statusCode').should('be.oneOf', [200, 201, 204]);
    cy.location('pathname', { timeout: 10_000 }).should('eq', '/feed');
    // Signal UX minimal que le feed est rendu
    cy.contains('button', 'Créer un article', { timeout: 10_000 }).should('be.visible');

    // --- SUBSCRIBE TOPIC ---
    // Act: s'assure qu'au moins un topic est abonné (commande idempotente)
    cy.ensureAtLeastOneSubscribedTopic();
    // Assert: la page topics montre au moins un "Déjà abonné"
    cy.contains('Déjà abonné', { timeout: 10_000 }).should('exist');

    // --- FEED (retour) + CREATE POST ---
    // Arrange: retour sur le feed
    cy.visit('/feed');
    cy.location('pathname', { timeout: 10_000 }).should('eq', '/feed');

    // Act: navigation vers la création depuis le feed
    cy.contains('button', 'Créer un article').click();
    cy.location('pathname', { timeout: 10_000 }).should('eq', '/posts/new');

    // Act: création d'article
    // 1) sélection d'un topic abonné (mat-option non disabled)
    cy.get('[data-testid="post-topic-select"]').click();
    cy.get('mat-option[aria-disabled="false"]', { timeout: 10_000 }).first().click();

    // 2) champs texte
    cy.get('[data-testid="post-title"]').type(title);
    cy.get('[data-testid="post-content"]').type(content);

    // 3) submit
    cy.get('[data-testid="post-submit"]').click();

    // Assert: redirection vers le détail /posts/:id + titre visible
    cy.location('pathname', { timeout: 10_000 }).should('match', /^\/posts\/\d+$/);
    cy.contains('h1.title', title, { timeout: 10_000 }).should('be.visible');

    // --- CREATE COMMENT ---
    // Act: commentaire sur le post nouvellement créé
    cy.get('[data-testid="comment-content"]').type(comment);
    cy.get('[data-testid="comment-submit"]').click();

    // Assert: commentaire affiché dans la liste
    cy.contains('.comment-bubble', comment, { timeout: 10_000 }).should('be.visible');

    // --- LOGOUT ---
    // Act + Assert: commande inclut l'assert de redirection vers '/'
    cy.logout();

    // Assert (bonne pratique) : un écran protégé redirige vers /login après logout
    cy.visit('/feed');
    cy.location('pathname', { timeout: 10_000 }).should('eq', '/login');
  });
});
