#!/usr/bin/env node
/**
 * Notebook publication profiling launcher. Runs the opt-in Cypress publication
 * profile scenarios (`e2e_test/features/cli/cli_notebook_web_created_note.feature`)
 * against an owned, disposable E2E stack. Delegates all stack lifecycle
 * (start/lease/shutdown) to `runE2eBatch`/`wireBatchCancellation`; this script
 * adds no service-lifecycle code of its own and is not a general benchmarking
 * framework — one script for one purpose.
 *
 * Env vars honored (all passed straight through to Cypress tasks, which read
 * `process.env` themselves; parsing/validation stays owned by those tasks):
 *   PUBLICATION_PROFILE_EXISTING           number of existing notes to seed
 *   PUBLICATION_PROFILE_UPDATES            number of existing notes to edit
 *   PUBLICATION_PROFILE_ADDITIONS          number of new notes to add
 *   PUBLICATION_PROFILE_FOLDERS            number of folders to distribute over
 *   PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS HTTP publication deadline
 *   PUBLICATION_PROFILE_TAGS               Cucumber tag expression override
 *
 * Defaults to the existing-note-edit HTTP profile (`@publicationProfileHttpUpdate`).
 * Set PUBLICATION_PROFILE_TAGS to run other profile scenarios instead, e.g.
 * '@publicationProfileHttp or @publicationProfileHttpRejection' for the small
 * addition/rejection regression proof.
 */
import { spawn } from 'node:child_process'
import { runE2eBatch, wireBatchCancellation } from '../e2e-runner.mjs'

const DEFAULT_TAGS = '@publicationProfileHttpUpdate'
const tags = process.env.PUBLICATION_PROFILE_TAGS ?? DEFAULT_TAGS

const requestTimeoutMs = Number(
  process.env.PUBLICATION_PROFILE_REQUEST_TIMEOUT_MS ?? 3_600_000
)
// Setup timeouts (fixture seeding, receiver verification) stay independent of
// the HTTP publication deadline: a fixed 10-minute margin on top of the
// deadline, not equal to it, so seeding never races the measured request.
const SETUP_MARGIN_MS = 600_000
const taskTimeoutMs = requestTimeoutMs + SETUP_MARGIN_MS
const commandTimeoutMs = requestTimeoutMs + SETUP_MARGIN_MS

// Seeding 1,000 existing notes plus one large HTTP publish is substantially
// slower than the 20/20 fixture the original spike launcher targeted at
// 360,000 ms; size the whole-batch budget up accordingly.
const BATCH_TIMEOUT_MS = 1_500_000

const cancellation = wireBatchCancellation()
try {
  process.exitCode = await runE2eBatch({
    argv: [
      '--spec',
      'e2e_test/features/cli/cli_notebook_web_created_note.feature',
      '--browser',
      'chrome',
    ],
    cancel: cancellation,
    timeoutMs: BATCH_TIMEOUT_MS,
    spawnCypress({ specs, cwd, env, cypressBin, configFile, stdio, browser }) {
      return spawn(
        process.execPath,
        [
          cypressBin,
          'run',
          '--config-file',
          configFile,
          '--spec',
          specs.join(','),
          '--browser',
          browser,
          '--config',
          `taskTimeout=${taskTimeoutMs},defaultCommandTimeout=${commandTimeoutMs}`,
          '--expose',
          `tags=${tags}`,
        ],
        { cwd, env, stdio }
      )
    },
  })
} finally {
  cancellation.detach()
}
