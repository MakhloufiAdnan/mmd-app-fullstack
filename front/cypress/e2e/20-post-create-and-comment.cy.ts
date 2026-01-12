describe('Posts + Comments', () => {
  before(() => cy.ensureE2EUserExists());

  beforeEach(() => {
    cy.session('e2e_user_session', () => cy.loginAsE2EUser());
  });

  it('creates a post then comments it', () => {
    cy.ensureAtLeastOneSubscribedTopic();

    cy.visit('/posts/new');

    cy.get('[data-testid="post-topic-select"]').click();
    cy.get('mat-option[aria-disabled="false"]', { timeout: 10_000 }).first().click();

    const ts = Date.now();
    const title = `Post E2E ${ts}`;
    const content = `Contenu E2E ${ts}`;

    cy.get('[data-testid="post-title"]').type(title);
    cy.get('[data-testid="post-content"]').type(content);
    cy.get('[data-testid="post-submit"]').click();

    cy.location('pathname', { timeout: 10_000 }).should('match', /^\/posts\/\d+$/);

    const comment = `Commentaire E2E ${ts}`;
    cy.get('[data-testid="comment-content"]').type(comment);
    cy.get('[data-testid="comment-submit"]').click();

    cy.contains('.comment-bubble', comment, { timeout: 10_000 }).should('be.visible');
  });
});
