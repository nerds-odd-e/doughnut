import { defineConfig } from 'cypress'
import createBundler from "@bahmutov/cypress-esbuild-preprocessor"
import { addCucumberPreprocessorPlugin } from "@badeball/cypress-cucumber-preprocessor"
import { createEsbuildPlugin } from "@badeball/cypress-cucumber-preprocessor/esbuild"

export default defineConfig({
  env: {
    TAGS: 'not @ignore and not @requiresDeveloperSecret',
  },
  chromeWebSecurity: false,
  screenshotOnRunFailure: true,
  pageLoadTimeout: 100000,
  defaultCommandTimeout: 6000,
  trashAssetsBeforeRuns: true,
  viewportWidth: 1000,
  viewportHeight: 660,
  environment: 'ci',
  e2e: {
    setupNodeEvents(on, config) {
      addCucumberPreprocessorPlugin(on, config)
      on(
        "file:preprocessor",
        createBundler({
          plugins: [createEsbuildPlugin(config)],
        }),
      )
      return config
    },
    experimentalRunAllSpecs: true,
    specPattern: 'cypress/e2e/**/*.feature',
    excludeSpecPattern: [
      '**/*.{js,ts}',
      '**/__snapshots__/*',
      '**/__image_snapshots__/*',
    ],
    baseUrl: 'http://localhost:5173',
  },
})
