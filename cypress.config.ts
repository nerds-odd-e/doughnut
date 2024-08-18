import { defineConfig } from 'cypress'
import commonConfig from './e2e_test/config/common'

export default defineConfig({
  ...commonConfig,
  env: {
    TAGS: 'not @ignore and not @requiresDeveloperSecret',
  },
  viewportWidth: 1200,
  viewportHeight: 800,
  e2e: {
    ...commonConfig.e2e,
    baseUrl: 'http://localhost:5173',
  },
})
