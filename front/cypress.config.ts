import { defineConfig } from 'cypress';

export default defineConfig({
  env: {
    e2eUsername: 'e2e_user',
    e2eEmail: 'e2e_user@example.test',
    e2ePassword: 'E2E!Passw0rd1',
    e2eAltPassword: 'E2E!Passw0rd2',
  },
  e2e: {
    baseUrl: 'http://localhost:4200',
    supportFile: 'cypress/support/e2e.ts',
    specPattern: 'cypress/e2e/**/*.cy.ts',
    viewportWidth: 1280,
    viewportHeight: 720,
    video: false,
    setupNodeEvents(on, config) {
      require('@cypress/code-coverage/task')(on, config);
      return config;
    },
  },
});
