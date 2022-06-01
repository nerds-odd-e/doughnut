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

import createBundler from "@bahmutov/cypress-esbuild-preprocessor"
import { createEsbuildPlugin } from "@badeball/cypress-cucumber-preprocessor/esbuild"
import NodeModulesPolyfills from "@esbuild-plugins/node-modules-polyfill"
import fs from "fs-extra"
import path from "path"

export default (on: Cypress.PluginEvents, config: Cypress.PluginConfigOptions): void => {
  on(
    "file:preprocessor",
    createBundler({
      plugins: [NodeModulesPolyfills(), createEsbuildPlugin(config)],
    }),
  )

  const file = config.env.configFile || "ci"
  console.table(`<<<<<< CYPRESS RUN ENV: ${file} >>>>>>`)
  return fs.readJson(path.resolve("cypress/config", `${file}.json`))
}
