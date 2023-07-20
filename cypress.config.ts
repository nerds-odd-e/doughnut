import { defineConfig } from 'cypress'
import commonConfig from './e2e_test/config/common'

export default defineConfig({
   ...commonConfig,
  env: {
    TAGS: 'not @ignore and not @requiresDeveloperSecret',
  },
  viewportWidth: 1000,
  viewportHeight: 660,
  e2e: {
    ...commonConfig.e2e,
    baseUrl: 'http://localhost:5173',
  },
})
