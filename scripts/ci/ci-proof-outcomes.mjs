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

const line = (runnerStatus, path) => `${status(runnerStatus)}\t${path}`

// Vitest's JSON reporter: a file that failed to load has no test results.
const vitest = (text) =>
  JSON.parse(text).testResults.flatMap((file) => {
    const path = relative(repoRoot, file.name)
    return file.assertionResults.length === 0 && file.status === 'failed'
      ? [line('failed', path)]
      : file.assertionResults.map((test) => line(test.status, path))
  })

// Cucumber's worst step status decides a test case, as in Cucumber itself.
const cucumberStatuses = [
  'UNKNOWN',
  'PASSED',
  'SKIPPED',
  'PENDING',
  'UNDEFINED',
  'AMBIGUOUS',
  'FAILED',
]
const cucumberStatus = (stepStatuses) => {
  const worst =
    cucumberStatuses[
      Math.max(0, ...stepStatuses.map((s) => cucumberStatuses.indexOf(s)))
    ]
  if (worst === 'PASSED') return 'passed'
  if (worst === 'UNKNOWN' || worst === 'SKIPPED') return 'other'
  return 'failed'
}

// Cucumber messages (NDJSON): one line per test case, from its final attempt.
const cucumber = (text) => {
  const envelopes = text
    .split('\n')
    .filter((json) => json.trim() !== '')
    .map((json) => JSON.parse(json))
  const pickleUris = new Map()
  const testCasePickles = new Map()
  const finalAttempts = new Map()
  const stepStatuses = new Map()
  for (const envelope of envelopes) {
    const { pickle, testCase, testCaseStarted, testStepFinished } = envelope
    if (pickle) pickleUris.set(pickle.id, pickle.uri)
    if (testCase) testCasePickles.set(testCase.id, testCase.pickleId)
    if (testCaseStarted) {
      const final = finalAttempts.get(testCaseStarted.testCaseId)
      if (!final || final.attempt < testCaseStarted.attempt) {
        finalAttempts.set(testCaseStarted.testCaseId, testCaseStarted)
      }
    }
    if (testStepFinished) {
      const { testCaseStartedId, testStepResult } = testStepFinished
      stepStatuses.set(testCaseStartedId, [
        ...(stepStatuses.get(testCaseStartedId) ?? []),
        testStepResult.status,
      ])
    }
  }
  return [...finalAttempts].map(([testCaseId, started]) =>
    line(
      cucumberStatus(stepStatuses.get(started.id) ?? []),
      pickleUris.get(testCasePickles.get(testCaseId))
    )
  )
}

const formats = { vitest, cucumber }

export const outcomeLines = (format, reportText) => formats[format](reportText)

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const [format, reportFile] = process.argv.slice(2)
  const lines = outcomeLines(format, readFileSync(reportFile, 'utf8'))
  process.stdout.write(lines.map((outcome) => `${outcome}\n`).join(''))
}
