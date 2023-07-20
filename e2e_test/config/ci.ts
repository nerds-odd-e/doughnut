/* eslint-disable @typescript-eslint/no-unused-vars */
import { defineConfig } from "cypress"
import commonConfig from "./common"

export default defineConfig({
  ...commonConfig,
  env: {
    TAGS: "not @ignore",
  },
  video: false,
  watchForFileChanges: false,
  e2e: {
    ...commonConfig.e2e,
    baseUrl: "http://localhost:9081",
  },
})
