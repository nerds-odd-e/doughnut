// Runs `node --test` with the given arguments. When CI_PROOF_NODE_REPORT_DIR
// names a directory, the run also writes the CI proof Node report there,
// named after the running package script (`<npm_lifecycle_event>.ndjson`),
// beside the usual spec output. The variable is not passed on, so tests that
// launch their own test runs stay outside this invocation's report.
//
//   node scripts/ci/node-test.mjs <node --test arguments>
import { spawnSync } from 'node:child_process'
import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const { CI_PROOF_NODE_REPORT_DIR: reportDir, ...env } = process.env
const reporter = fileURLToPath(new URL('node-test-report.mjs', import.meta.url))
if (reportDir) mkdirSync(reportDir, { recursive: true })
const reportArgs = reportDir
  ? [
      '--test-reporter=spec',
      '--test-reporter-destination=stdout',
      `--test-reporter=${reporter}`,
      `--test-reporter-destination=${join(reportDir, `${env.npm_lifecycle_event}.ndjson`)}`,
    ]
  : []
const { status } = spawnSync(
  process.execPath,
  ['--test', ...reportArgs, ...process.argv.slice(2)],
  { stdio: 'inherit', env }
)
process.exit(status ?? 1)
