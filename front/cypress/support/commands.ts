/// <reference types="cypress" />

function xsrfHeaderFromCookie(): Cypress.Chainable<{ 'X-XSRF-TOKEN': string }> {
  return cy.getCookie('XSRF-TOKEN').then((cookie) => {
    if (!cookie?.value) {
      throw new Error("Cookie 'XSRF-TOKEN' introuvable. Back/proxy OK ? (GET /api/auth/csrf)");
    }
    // Certains navigateurs encodent la valeur
    const token = decodeURIComponent(cookie.value);
    return { 'X-XSRF-TOKEN': token };
  });
}

Cypress.Commands.add('ensureE2EUserExists', () => {
  const username = Cypress.env('e2eUsername') as string;
  const email = Cypress.env('e2eEmail') as string;
  const password = Cypress.env('e2ePassword') as string;

  // 1) Récupère CSRF (cookie XSRF-TOKEN)
  cy.request('GET', '/api/auth/csrf');

  // 2) Tente register (OK si 201, ou 409 si déjà existant)
  xsrfHeaderFromCookie().then((headers) => {
    cy.request({
      method: 'POST',
      url: '/api/auth/register',
      headers,
      body: { email, username, password },
      failOnStatusCode: false,
    }).then((res) => {
      expect([201, 409]).to.include(res.status);
    });
  });
});

Cypress.Commands.add('loginAsE2EUser', (password?: string) => {
  const identifier = Cypress.env('e2eUsername') as string;
  const primaryPwd = password ?? (Cypress.env('e2ePassword') as string);
  const altPwd = Cypress.env('e2eAltPassword') as string;

  const attempt = (pwd: string): Cypress.Chainable<boolean> => {
    cy.intercept('POST', '**/api/auth/login').as('login');

    cy.get('[data-testid="login-identifier"]').clear().type(identifier);
    cy.get('[data-testid="login-password"]').clear().type(pwd, { log: false });
    cy.get('[data-testid="login-submit"]').click();

    return cy.wait('@login').then((i) => {
      const status = i.response?.statusCode ?? 0;
      const ok = [200, 201, 204].includes(status);

      // Important: on ne return jamais un bool sync
      return cy.wrap(ok, { log: false });
    });
  };

  cy.visit('/login');

  attempt(primaryPwd).then((okPrimary) => {
    if (okPrimary) {
      cy.location('pathname', { timeout: 10_000 }).should('eq', '/feed');
      return;
    }

    attempt(altPwd).then((okAlt) => {
      expect(okAlt, 'login should work with primary OR alt password').to.eq(true);
      cy.location('pathname', { timeout: 10_000 }).should('eq', '/feed');
    });
  });
});

Cypress.Commands.add('logout', () => {
  // 1) Si le bouton logout desktop est visible, on clique
  cy.get('body').then(($body) => {
    const $visibleLogout = $body.find('[data-testid="nav-logout"]:visible');

    if ($visibleLogout.length > 0) {
      cy.wrap($visibleLogout.first()).click();
      cy.location('pathname', { timeout: 10_000 }).should('eq', '/');
      return;
    }

    // 2) Sinon on ouvre le burger puis logout dans le drawer
    const $burger = $body.find('[data-testid="nav-burger"]:visible');
    if ($burger.length === 0) {
      throw new Error('Burger introuvable (data-testid="nav-burger")');
    }

    cy.wrap($burger.first()).click();

    // Le drawer peut prendre un petit délai à s’afficher
    cy.get('[data-testid="nav-logout"]:visible', { timeout: 10_000 }).first().click();

    cy.location('pathname', { timeout: 10_000 }).should('eq', '/');
  });
});

Cypress.Commands.add('ensureAtLeastOneSubscribedTopic', () => {
  cy.visit('/topics');

  // Si on a été redirigé (pas auth), on relog puis on revient
  cy.location('pathname', { timeout: 10_000 }).then((path) => {
    if (path === '/login') {
      cy.loginAsE2EUser();
      cy.visit('/topics');
    }
  });

  // Assert explicite qu'on est bien sur /topics (sinon debug clair)
  cy.location('pathname', { timeout: 10_000 }).should('eq', '/topics');

  cy.get('[data-testid^="topic-subscribe-"]', { timeout: 10_000 }).then(($btns) => {
    const hasSubscribed = [...$btns].some((b) => (b.textContent ?? '').includes('Déjà abonné'));
    if (hasSubscribed) return;

    const enabled = [...$btns].find((b) => !(b as HTMLButtonElement).disabled);
    if (!enabled) return;

    cy.wrap(enabled).click();
    cy.wrap(enabled).should('contain.text', 'Déjà abonné');
  });
});

declare global {
  namespace Cypress {
    interface Chainable {
      ensureE2EUserExists(): Chainable<void>;
      loginAsE2EUser(password?: string): Chainable<void>;
      logout(): Chainable<void>;
      ensureAtLeastOneSubscribedTopic(): Chainable<void>;
    }
  }
}

export {};
