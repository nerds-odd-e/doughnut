import { defineConfig } from 'cypress'
import commonConfig from './common'
import { cucumberTagExpression } from './ciTagSelection.mjs'

export default defineConfig({
  ...commonConfig,
  expose: {
    tags: cucumberTagExpression(process.env.CI),
    RECORD_E2E_TIMING: process.env.RECORD_E2E_TIMING,
    // The CI workflow names an absolute path for the CI proof summary.
    ...(process.env.CI_PROOF_CUCUMBER_MESSAGES
      ? {
          messagesEnabled: true,
          messagesOutput: process.env.CI_PROOF_CUCUMBER_MESSAGES,
        }
      : {}),
  },
  video: false,
  watchForFileChanges: false,
})
