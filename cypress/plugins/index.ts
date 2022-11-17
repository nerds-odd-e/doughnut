// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)

import createBundler from "@bahmutov/cypress-esbuild-preprocessor"
import { addCucumberPreprocessorPlugin } from "@badeball/cypress-cucumber-preprocessor"
import createEsbuildPlugin from "@badeball/cypress-cucumber-preprocessor/esbuild"

export default async (on: Cypress.PluginEvents, config: Cypress.PluginConfigOptions): void => {
  await addCucumberPreprocessorPlugin(on, config)
  on(
    "file:preprocessor",
    createBundler({
      plugins: [createEsbuildPlugin(config)],
    }),
  )
  return config
}
