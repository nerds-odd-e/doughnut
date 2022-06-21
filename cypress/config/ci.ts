import { defineConfig } from "cypress"

export default defineConfig({
  env: {
    TAGS: "not @ignore and not @slow",
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
    specPattern: "cypress/integration/**/*.feature",
    excludeSpecPattern: ["**/*.{js,ts}", "**/__snapshots__/*", "**/__image_snapshots__/*"],
    baseUrl: "http://localhost:9081",
  },
})
