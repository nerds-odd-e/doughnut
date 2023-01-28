/* eslint-disable @typescript-eslint/no-unused-vars */
import { defineConfig } from "cypress"
import createBundler from "@bahmutov/cypress-esbuild-preprocessor"
import { addCucumberPreprocessorPlugin } from "@badeball/cypress-cucumber-preprocessor"
import { createEsbuildPlugin } from "@badeball/cypress-cucumber-preprocessor/esbuild"

async function setupNodeEvents(
  on: Cypress.PluginEvents,
  config: Cypress.PluginConfigOptions,
): Promise<Cypress.PluginConfigOptions> {
  await addCucumberPreprocessorPlugin(on, config)

  on(
    "file:preprocessor",
    createBundler({
      plugins: [createEsbuildPlugin(config)],
    }),
  )

  require("../plugins/index.ts").default(on, config)
  return config
}

export default defineConfig({
  env: {
    TAGS: "not @ignore",
  },
  chromeWebSecurity: false,
  screenshotOnRunFailure: true,
  video: false,
  pageLoadTimeout: 100000,
  defaultCommandTimeout: 6000,
  trashAssetsBeforeRuns: true,
  watchForFileChanges: false,
  environment: "ci",
  e2e: {
    setupNodeEvents(on, config) {
      return require("../plugins/index.ts").default(on, config)
    },
    specPattern: "cypress/e2e/**/*.feature",
    excludeSpecPattern: ["**/*.{js,ts}", "**/__snapshots__/*", "**/__image_snapshots__/*"],
    baseUrl: "http://localhost:9081",
  },
})
