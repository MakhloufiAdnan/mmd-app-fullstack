describe('Profile - Unsubscribe', () => {
  before(() => cy.ensureE2EUserExists());

  beforeEach(() => {
    cy.session('e2e_user_session', () => cy.loginAsE2EUser(), { cacheAcrossSpecs: true });
  });

  it('unsubscribes from a topic (idempotent)', () => {
    // 1) S'assure qu'il existe au moins 1 abonnement (par /topics)
    cy.ensureAtLeastOneSubscribedTopic();

    // 2) Désabonnement via /profile (c'est ton UX)
    cy.visit('/profile');
    cy.location('pathname', { timeout: 10_000 }).should('eq', '/profile');

    // 3) Attend au moins un bouton "Se désabonner", clique le premier, vérifie disparition
    cy.get('[data-testid^="profile-unsubscribe-"]', { timeout: 10_000 })
      .should('have.length.greaterThan', 0)
      .first()
      .invoke('attr', 'data-testid')
      .then((testId) => {
        expect(testId, 'unsubscribe testid').to.be.a('string');

        cy.get(`[data-testid="${testId}"]`).click();
        cy.get(`[data-testid="${testId}"]`, { timeout: 10_000 }).should('not.exist');
      });
  });
});
