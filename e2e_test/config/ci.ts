import { defineConfig } from 'cypress'
import commonConfig from './common'

// One home for the opt-in publication profile tags excluded from ordinary
// runs, so adding a new profile scenario edits this list once instead of
// both the CI and local tag expressions below.
const publicationProfileTags = [
  '@publicationProfile',
  '@publicationProfileRejection',
  '@publicationProfileHttp',
  '@publicationProfileHttpRejection',
  '@publicationProfileHttpUpdate',
]
const excludeTags = (...tags: string[]) =>
  tags.map((tag) => `not ${tag}`).join(' and ')

export default defineConfig({
  ...commonConfig,
  expose: {
    tags: process.env.CI
      ? excludeTags('@ignore', '@wip', ...publicationProfileTags)
      : excludeTags('@ignore', ...publicationProfileTags),
    RECORD_E2E_TIMING: process.env.RECORD_E2E_TIMING,
    // The CI workflow names an absolute path for the CI proof summary.
    ...(process.env.CI_PROOF_CUCUMBER_MESSAGES
      ? {
          messagesEnabled: true,
          messagesOutput: process.env.CI_PROOF_CUCUMBER_MESSAGES,
        }
      : {}),
  },
  video: false,
  watchForFileChanges: false,
})
