// Runs `node --test` with the given arguments. When CI_PROOF_NODE_REPORT names
// a file, the run also writes the CI proof Node report there beside the usual
// spec output. The variable is not passed on, so tests that launch their own
// test runs stay outside this invocation's report.
//
//   node scripts/ci/node-test.mjs <node --test arguments>
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const { CI_PROOF_NODE_REPORT: report, ...env } = process.env
const reporter = fileURLToPath(new URL('node-test-report.mjs', import.meta.url))
const reportArgs = report
  ? [
      '--test-reporter=spec',
      '--test-reporter-destination=stdout',
      `--test-reporter=${reporter}`,
      `--test-reporter-destination=${report}`,
    ]
  : []
const { status } = spawnSync(
  process.execPath,
  ['--test', ...reportArgs, ...process.argv.slice(2)],
  { stdio: 'inherit', env }
)
process.exit(status ?? 1)
