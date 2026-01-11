const path = require('node:path');

module.exports = function karmaConfig(config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],

    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma'),
    ],

    client: {
      clearContext: false,
    },

    jasmineHtmlReporter: {
      suppressAll: true,
    },

    reporters: ['progress', 'kjhtml', 'coverage'],

    coverageReporter: {
      dir: path.join(__dirname, './coverage'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'lcovonly', file: 'lcov.info' },
        { type: 'text-summary' },
      ],
      fixWebpackSourcePaths: true,
    },

    browsers: ['ChromeHeadless'],
    singleRun: true,

    browserDisconnectTolerance: 2,
    browserDisconnectTimeout: 10000,
    browserNoActivityTimeout: 60000,

    restartOnFileChange: false,
  });
};
