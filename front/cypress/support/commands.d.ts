/// <reference types="cypress" />

declare global {
  namespace Cypress {
    interface Chainable<Subject = any> {
      ensureE2EUserExists(): Chainable<void>;
      loginAsE2EUser(password?: string): Chainable<void>;
      logout(): Chainable<void>;
      ensureAtLeastOneSubscribedTopic(): Chainable<void>;
    }
  }
}

export {};
