'use strict';

const path = require('node:path');

/**
 * Instrumentation Istanbul uniquement pour le build "coverage".
 * Résultat attendu : window.__coverage__ dans le navigateur.
 */
function instrumentForCoverage(config) {
  config.module = config.module || {};
  config.module.rules = config.module.rules || [];

  config.module.rules.push({
    test: /\.[jt]s$/,
    enforce: 'post',
    include: path.join(__dirname, 'src'),
    exclude: [/node_modules/, /\.spec\.[jt]s$/, /cypress/],
    use: {
      loader: '@jsdevtools/coverage-istanbul-loader',
      options: { esModules: true },
    },
  });

  return config;
}

module.exports = instrumentForCoverage;
