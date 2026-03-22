import { defineConfig } from 'cypress'
import commonConfig from './common'

export default defineConfig({
  ...commonConfig,
  env: {
    TAGS: 'not @ignore',
  },
  video: false,
  watchForFileChanges: false,
})
