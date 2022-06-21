import { defineConfig } from "cypress"

export default defineConfig({
  env: {
    TAGS: "not @ignore and not @slow",
  },
  chromeWebSecurity: false,
  screenshotOnRunFailure: true,
  video: true,
  pageLoadTimeout: 100000,
  defaultCommandTimeout: 6000,
  trashAssetsBeforeRuns: true,
  viewportWidth: 1000,
  viewportHeight: 660,
  environment: "development",
  e2e: {
    setupNodeEvents(on, config) {
      return require("../plugins/index.ts").default(on, config)
    },
    specPattern: "cypress/integration/**/*.feature",
    excludeSpecPattern: ["**/*.{js,ts}", "**/__snapshots__/*", "**/__image_snapshots__/*"],
    baseUrl: "http://localhost:9081",
  },
})
