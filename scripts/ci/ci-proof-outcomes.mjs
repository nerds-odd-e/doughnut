// Converts a test runner's own report into the CI proof collector's outcome
// lines: one `<status><TAB><repo-relative path>` per result in the declared unit, where status
// is `passed`, `failed` or `other`. An optional times file gets one
// `<seconds><TAB><repo-relative path>` line per timed check.
//
//   node scripts/ci/ci-proof-outcomes.mjs <format> <report file or dir> [times file] [case|file]
import { readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const repoRoot = fileURLToPath(new URL('../..', import.meta.url))

const status = (runnerStatus) =>
  runnerStatus === 'passed' || runnerStatus === 'failed'
    ? runnerStatus
    : 'other'

// Vitest's JSON reporter: a file that failed to load has no test results.
const vitest = (text) =>
  JSON.parse(text).testResults.flatMap((file) => {
    const path = relative(repoRoot, file.name)
    return file.assertionResults.length === 0 && file.status === 'failed'
      ? [{ status: 'failed', path }]
      : file.assertionResults.map((test) => ({
          status: test.status,
          path,
          seconds:
            typeof test.duration === 'number'
              ? test.duration / 1000
              : undefined,
        }))
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

const cucumberSeconds = (steps) =>
  steps.reduce(
    (total, { duration }) =>
      duration ? (total ?? 0) + duration.seconds + duration.nanos / 1e9 : total,
    undefined
  )

// Cucumber messages (NDJSON): one check per test case, from its final attempt.
const cucumber = (text) => {
  const envelopes = text
    .split('\n')
    .filter((json) => json.trim() !== '')
    .map((json) => JSON.parse(json))
  const pickleUris = new Map()
  const testCasePickles = new Map()
  const finalAttempts = new Map()
  const stepResults = new Map()
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
      stepResults.set(testCaseStartedId, [
        ...(stepResults.get(testCaseStartedId) ?? []),
        testStepResult,
      ])
    }
  }
  return [...finalAttempts].map(([testCaseId, started]) => {
    const steps = stepResults.get(started.id) ?? []
    return {
      status: cucumberStatus(steps.map((step) => step.status)),
      path: pickleUris.get(testCasePickles.get(testCaseId)),
      seconds: cucumberSeconds(steps),
    }
  })
}

const attribute = (attrs, name) => {
  const match = attrs.match(new RegExp(`\\b${name}="([^"]*)"`))
  return match?.[1]
}

const junitPath = (classname) => {
  const outer = classname.split('$', 1)[0]
  return `backend/src/test/java/${outer.replaceAll('.', '/')}.java`
}

const junitStatus = (body) => {
  if (/<(?:failure|error)\b/.test(body)) return 'failed'
  if (/<skipped\b/.test(body)) return 'other'
  return 'passed'
}

// Gradle JUnit XML: one <testcase> per method; nested $ classes share the outer file.
const junit = (text) =>
  [...text.matchAll(/<testcase\b([^>/]*)(?:\/>|>([\s\S]*?)<\/testcase>)/g)].map(
    ([, attrs, body = '']) => {
      const time = attribute(attrs, 'time')
      return {
        status: junitStatus(body),
        path: junitPath(attribute(attrs, 'classname')),
        seconds: time === undefined ? undefined : Number(time),
      }
    }
  )

// The Bash script runner's CI_PROOF_BASH_REPORT: one
// `<passed|failed><TAB><seconds><TAB><repo-relative path>` line per test file.
const bash = (text) =>
  text
    .split('\n')
    .filter((line) => line !== '')
    .map((line) => {
      const match = line.match(/^(passed|failed)\t(\d+\.\d+)\t(\S.*)$/)
      if (!match) throw new Error(`malformed Bash report line: ${line}`)
      const [, status, seconds, path] = match
      return { status, path, seconds: Number(seconds) }
    })

// scripts/ci/node-test-report.mjs: one JSON check per line, seconds optional.
const node = (text) =>
  text
    .split('\n')
    .filter((line) => line !== '')
    .map((line) => {
      const { status, path, seconds, unit } = JSON.parse(line)
      if (
        !(
          ['passed', 'failed', 'other'].includes(status) &&
          ['case', 'file'].includes(unit)
        ) ||
        typeof path !== 'string' ||
        !['number', 'undefined'].includes(typeof seconds)
      ) {
        throw new Error(`malformed Node report line: ${line}`)
      }
      return { status, path, seconds, unit }
    })

const formats = { vitest, cucumber, junit, bash, node }

const junitChecksFrom = (reportPath) => {
  if (!statSync(reportPath).isDirectory()) {
    return junit(readFileSync(reportPath, 'utf8'))
  }
  return readdirSync(reportPath)
    .filter((name) => name.startsWith('TEST-') && name.endsWith('.xml'))
    .sort()
    .flatMap((name) => junit(readFileSync(join(reportPath, name), 'utf8')))
}

const checksFrom = (format, reportPath) =>
  format === 'junit'
    ? junitChecksFrom(reportPath)
    : formats[format](readFileSync(reportPath, 'utf8'))

const outcomesOf = (checks) =>
  checks.map((check) => `${status(check.status)}\t${check.path}`)

const inUnit = (format, checks, unit) =>
  unit
    ? checks.filter(
        (check) =>
          (check.unit ?? (format === 'bash' ? 'file' : 'case')) === unit
      )
    : checks

export const outcomeLines = (format, reportText, unit) =>
  outcomesOf(inUnit(format, formats[format](reportText), unit))

const textOf = (lines) => lines.map((line) => `${line}\n`).join('')

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const [format, reportFile, timesFile, unit] = process.argv.slice(2)
  const checks = inUnit(format, checksFrom(format, reportFile), unit)
  if (timesFile) {
    const times = checks
      .filter(({ seconds }) => seconds !== undefined)
      .map(({ seconds, path }) => `${seconds}\t${path}`)
    writeFileSync(timesFile, textOf(times))
  }
  process.stdout.write(textOf(outcomesOf(checks)))
}
