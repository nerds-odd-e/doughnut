// Converts a test runner's own report into the CI proof collector's outcome
// lines: one `<status><TAB><repo-relative path>` per test case, where status
// is `passed`, `failed` or `other`.
//
//   node scripts/ci/ci-proof-outcomes.mjs <format> <report file>
import { readFileSync } from 'node:fs'
import { relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const repoRoot = fileURLToPath(new URL('../..', import.meta.url))

const status = (runnerStatus) =>
  runnerStatus === 'passed' || runnerStatus === 'failed'
    ? runnerStatus
    : 'other'

const line = (runnerStatus, path) =>
  `${status(runnerStatus)}\t${relative(repoRoot, path)}`

// Vitest's JSON reporter: a file that failed to load has no test results.
const vitest = (report) =>
  report.testResults.flatMap((file) =>
    file.assertionResults.length === 0 && file.status === 'failed'
      ? [line('failed', file.name)]
      : file.assertionResults.map((test) => line(test.status, file.name))
  )

const formats = { vitest }

export const outcomeLines = (format, report) => formats[format](report)

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const [format, reportFile] = process.argv.slice(2)
  const lines = outcomeLines(
    format,
    JSON.parse(readFileSync(reportFile, 'utf8'))
  )
  process.stdout.write(lines.map((outcome) => `${outcome}\n`).join(''))
}
