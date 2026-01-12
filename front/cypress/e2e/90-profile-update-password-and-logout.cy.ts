describe('Profile - Update password + logout (reversible)', () => {
  before(() => cy.ensureE2EUserExists());

  const loginWithPassword = (pwd: string): Cypress.Chainable<boolean> => {
    const identifier = Cypress.env('e2eUsername') as string;

    cy.visit('/login');
    cy.intercept('POST', '**/api/auth/login').as('login');

    cy.get('[data-testid="login-identifier"]').clear().type(identifier);
    cy.get('[data-testid="login-password"]').clear().type(pwd, { log: false });
    cy.get('[data-testid="login-submit"]').click();

    return cy
      .wait('@login')
      .then((i) => {
        const status = i.response?.statusCode ?? 0;
        const ok = [200, 201, 204].includes(status);
        return cy.wrap(ok, { log: false });
      })
      .then((ok) => {
        cy.location('pathname', { timeout: 10_000 }).should('eq', ok ? '/feed' : '/login');
        return cy.wrap(ok, { log: false });
      });
  };

  const getWorkingPassword = (original: string, alt: string): Cypress.Chainable<string> => {
    return loginWithPassword(original).then((okOriginal) => {
      if (okOriginal) {
        return cy.wrap(original, { log: false });
      }

      return loginWithPassword(alt).then((okAlt) => {
        expect(okAlt, 'login should work with original OR alt').to.eq(true);
        return cy.wrap(alt, { log: false });
      });
    });
  };

  const updatePasswordFromProfile = (newPassword: string): void => {
    cy.visit('/profile');
    cy.location('pathname', { timeout: 10_000 }).should('eq', '/profile');

    cy.get('[data-testid="profile-password"]')
      .scrollIntoView()
      .click({ force: true })
      .clear({ force: true })
      .type(newPassword, { force: true, log: false });

    cy.get('[data-testid="profile-save"]').click();

    //  laisse le temps au back de terminer 
    cy.wait(800);
  };

  it('updates password, validates login, and ends with original password', () => {
    const original = Cypress.env('e2ePassword') as string;
    const alt = Cypress.env('e2eAltPassword') as string;

    getWorkingPassword(original, alt).then((currentPwd: string) => {
      const nextPwd = currentPwd === original ? alt : original;

      updatePasswordFromProfile(nextPwd);
      cy.logout();

      loginWithPassword(nextPwd).then((okNext) => {
        expect(okNext, 'login with updated password').to.eq(true);

        // termine toujours sur le password "original" pour rendre le test idempotent
        if (nextPwd !== original) {
          updatePasswordFromProfile(original);
          cy.logout();

          loginWithPassword(original).then((okBack) => {
            expect(okBack, 'login after rollback to original').to.eq(true);
            cy.logout();
          });

          return;
        }

        cy.logout();
      });
    });
  });
});
