module.exports = function (config) {
  config.set({
    browsers: ['ChromeHeadless'],
    singleRun: true,

    browserDisconnectTolerance: 2,
    browserDisconnectTimeout: 10000,
    browserNoActivityTimeout: 60000,

    client: {
      clearContext: false
    }
  });
};
