// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

/// <reference types="cypress" />

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)

const cucumber = require("cypress-cucumber-preprocessor").default;
const browserify = require("@cypress/browserify-preprocessor");
// const getCompareSnapshotsPlugin = require('cypress-image-diff-js/dist/plugin');
const fs = require("fs-extra");
const path = require("path");
/**
 * @type {Cypress.PluginConfig}
 */
module.exports = (on, config) => {
  // `on` is used to hook into various events Cypress emits
  // `config` is the resolved Cypress config
  // getCompareSnapshotsPlugin(on, config);
  const options = browserify.defaultOptions;
  options.browserifyOptions.plugin.unshift(["tsify"]);
  on("file:preprocessor", cucumber(options));

  const file = config.env.configFile || "ci";
  console.table(`<<<<<< CYPRESS RUN ENV: ${file} >>>>>>`);
  return getConfigurationByFile(file);
};

function getConfigurationByFile(file) {
  const pathToConfigFile = path.resolve("cypress/config", `${file}.json`);

  return fs.readJson(pathToConfigFile);
}
