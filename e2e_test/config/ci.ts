import { defineConfig } from 'cypress'
import commonConfig from './common'

export default defineConfig({
  ...commonConfig,
  expose: {
    tags: process.env.CI
      ? 'not @ignore and not @wip and not @publicationProfile and not @publicationProfileRejection and not @publicationProfileHttp and not @publicationProfileHttpRejection'
      : 'not @ignore and not @publicationProfile and not @publicationProfileRejection and not @publicationProfileHttp and not @publicationProfileHttpRejection',
    RECORD_E2E_TIMING: process.env.RECORD_E2E_TIMING,
  },
  video: false,
  watchForFileChanges: false,
})
