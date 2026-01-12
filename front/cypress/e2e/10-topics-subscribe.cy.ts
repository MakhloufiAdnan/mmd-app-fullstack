describe('Topics', () => {
  before(() => cy.ensureE2EUserExists());

  beforeEach(() => {
    cy.session('e2e_user_session', () => cy.loginAsE2EUser());
  });

  it('subscribes to a topic (idempotent)', () => {
    cy.ensureAtLeastOneSubscribedTopic();
    cy.contains('Déjà abonné').should('exist');
  });
});
