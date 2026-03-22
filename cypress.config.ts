import { defineConfig } from 'cypress'
import commonConfig from './e2e_test/config/common'

export default defineConfig({
  ...commonConfig,
  env: {
    TAGS: 'not @ignore',
  },
  expose: {
    RECORD_E2E_TIMING: process.env.RECORD_E2E_TIMING,
  },
  viewportWidth: 1200,
  viewportHeight: 800,
})
