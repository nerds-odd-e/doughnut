/* eslint-disable @typescript-eslint/no-unused-vars */
import createBundler from "@bahmutov/cypress-esbuild-preprocessor"
import { addCucumberPreprocessorPlugin } from "@badeball/cypress-cucumber-preprocessor"
import createEsbuildPlugin from "@badeball/cypress-cucumber-preprocessor/esbuild"

const commonConfig = {
  chromeWebSecurity: false,
  screenshotOnRunFailure: true,
  fixturesFolder: "e2e_test/fixtures",
  screenshotsFolder: "e2e_test/screenshots",
  pageLoadTimeout: 100000,
  defaultCommandTimeout: 6000,
  trashAssetsBeforeRuns: true,
  environment: "ci",
  e2e: {
    setupNodeEvents(
      on: Cypress.PluginEvents,
      config: Cypress.PluginConfigOptions,
    ): Promise<Cypress.PluginConfigOptions> {
      addCucumberPreprocessorPlugin(on, config)
      on(
        "file:preprocessor",
        createBundler({
          plugins: [createEsbuildPlugin(config)],
        }),
      )
      return config
    },
    supportFile: "e2e_test/support/e2e.ts",
    specPattern: "e2e_test/features/**/*.feature",
    excludeSpecPattern: ["**/*.{js,ts}", "**/__snapshots__/*", "**/__image_snapshots__/*"],
  },
}

export default commonConfig
