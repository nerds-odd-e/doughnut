import { defineConfig } from 'cypress'
import commonConfig from './common'

export default defineConfig({
  ...commonConfig,
  expose: {
    tags: process.env.CI ? 'not @ignore and not @wip' : 'not @ignore',
    RECORD_E2E_TIMING: process.env.RECORD_E2E_TIMING,
  },
  video: false,
  watchForFileChanges: false,
})
