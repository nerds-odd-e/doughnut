import { afterEach, vi } from 'vitest'
import { describeNotebookPublishAncestry } from './notebookPublish.ancestry.suite.js'
import { describeNotebookPublishBinding } from './notebookPublish.binding.suite.js'
import { describeNotebookPublishReadiness } from './notebookPublish.readiness.suite.js'
import { describeNotebookPublishSubmission } from './notebookPublish.submission.suite.js'

// Keep every publish-flow suite registered from this one test entrypoint. The ancestry
// leak-detection assertions snapshot a shared tmpdir prefix, so running sibling .test.ts files
// concurrently could make another worker's staging directory look like a leak.
afterEach(() => {
  vi.unstubAllGlobals()
})

describeNotebookPublishBinding()
describeNotebookPublishReadiness()
describeNotebookPublishAncestry()
describeNotebookPublishSubmission()
