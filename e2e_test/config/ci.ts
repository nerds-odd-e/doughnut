import { defineConfig } from 'cypress'
import commonConfig from './common'

export default defineConfig({
  ...commonConfig,
  env: {
    tags: process.env.CI ? 'not @ignore and not @wip' : 'not @ignore',
  },
  video: false,
  watchForFileChanges: false,
})
