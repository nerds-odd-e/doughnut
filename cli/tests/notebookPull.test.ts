import { describeNotebookPullAcceptedHistory } from './notebookPull.acceptedHistory.suite.js'
import { describeNotebookPullLfsRefusal } from './notebookPull.lfsRefusal.suite.js'
import { describeNotebookPullReadiness } from './notebookPull.readiness.suite.js'

// Keep the pull suites registered from one test entrypoint. Accepted-history leak detection
// snapshots a shared tmpdir prefix, so separate workers could otherwise look like one leaked.
describeNotebookPullReadiness()
describeNotebookPullLfsRefusal()
describeNotebookPullAcceptedHistory()
