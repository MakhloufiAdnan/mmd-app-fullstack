describe('Register (unique user)', () => {
  it('registers a new unique user and redirects to /login', () => {
    const ts = Date.now();
    const username = `e2e_user_${ts}`;
    const email = `e2e_${ts}@example.test`;
    const password = 'E2E!Passw0rd1';

    cy.visit('/register');
    cy.get('[data-testid="register-username"]').type(username);
    cy.get('[data-testid="register-email"]').type(email);
    cy.get('[data-testid="register-password"]').type(password);
    cy.get('[data-testid="register-submit"]').click();

    cy.location('pathname', { timeout: 10_000 }).should('eq', '/login');
  });
});
