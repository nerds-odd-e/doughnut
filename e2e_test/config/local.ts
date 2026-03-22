/* eslint-disable @typescript-eslint/no-unused-vars */
import { defineConfig } from 'cypress'
import commonConfig from './common'

export default defineConfig({
  ...commonConfig,
  env: {
    TAGS: '@focus and not @ignore',
  },
  viewportWidth: 1200,
  viewportHeight: 800,
})
