import { defineConfig } from 'cypress'
import commonConfig from './common'

export default defineConfig({
  ...commonConfig,
  expose: {
    tags: process.env.CI
      ? 'not @ignore and not @wip and not @publicationProfile and not @publicationProfileRejection'
      : 'not @ignore and not @publicationProfile and not @publicationProfileRejection',
    RECORD_E2E_TIMING: process.env.RECORD_E2E_TIMING,
  },
  video: false,
  watchForFileChanges: false,
})
