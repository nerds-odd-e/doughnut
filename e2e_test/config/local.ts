import { defineConfig } from 'cypress'
import commonConfig from './common'

export default defineConfig({
  ...commonConfig,
  env: {
    TAGS: '@focus and not @ignore',
  },
})
