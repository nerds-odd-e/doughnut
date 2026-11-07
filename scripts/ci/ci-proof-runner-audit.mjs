import assert from 'node:assert/strict'
import { readFileSync, readdirSync } from 'node:fs'
import { join, relative } from 'node:path'

const [format, reportPath, fragmentDir] = process.argv.slice(2)
const report = readFileSync(reportPath, 'utf8')
const [fragmentName, ...extraFragments] = readdirSync(fragmentDir)
assert.deepEqual(extraFragments, [])
const fragment = JSON.parse(
  readFileSync(join(fragmentDir, fragmentName), 'utf8')
)
const files = new Map()
const emptyFigures = () => ({ passed: 0, failed: 0, other: 0, untimed: 0 })
function record(path, status, durationMs) {
  if (!path.startsWith(`${fragment.entry}/`)) return
  const figures = files.get(path) ?? emptyFigures()
  figures[status] += 1
  if (durationMs === undefined) figures.untimed += 1
  else figures.durationMs = (figures.durationMs ?? 0) + durationMs
  files.set(path, figures)
}

if (format === 'vitest') {
  for (const file of JSON.parse(report).testResults) {
    const path = relative(process.cwd(), file.name)
    if (!file.assertionResults.length && file.status === 'failed') {
      record(path, 'failed')
    }
    for (const assertion of file.assertionResults) {
      const status = ['passed', 'failed'].includes(assertion.status)
        ? assertion.status
        : 'other'
      record(path, status, assertion.duration ?? undefined)
    }
  }
} else {
  assert.equal(format, 'cucumber')
  const envelopes = report
    .split('\n')
    .filter((line) => line.trim() !== '')
    .map((line) => JSON.parse(line))
  const pickles = new Map()
  const cases = new Map()
  const starts = []
  const stepsByStart = new Map()
  for (const {
    pickle,
    testCase,
    testCaseStarted,
    testStepFinished,
  } of envelopes) {
    if (pickle) pickles.set(pickle.id, pickle.uri)
    if (testCase) cases.set(testCase.id, testCase.pickleId)
    if (testCaseStarted) starts.push(testCaseStarted)
    if (testStepFinished) {
      const { testCaseStartedId, testStepResult } = testStepFinished
      const steps = stepsByStart.get(testCaseStartedId) ?? []
      steps.push(testStepResult)
      stepsByStart.set(testCaseStartedId, steps)
    }
  }
  const finalByCase = new Map(
    starts
      .sort((a, b) => a.attempt - b.attempt)
      .map((start) => [start.testCaseId, start.id])
  )
  for (const [caseId, startId] of finalByCase) {
    const steps = stepsByStart.get(startId) ?? []
    const statuses = new Set(steps.map((step) => step.status))
    const failed = ['PENDING', 'UNDEFINED', 'AMBIGUOUS', 'FAILED'].some(
      (value) => statuses.has(value)
    )
    const status = failed
      ? 'failed'
      : statuses.has('PASSED') && !statuses.has('SKIPPED')
        ? 'passed'
        : 'other'
    const durations = steps.map((step) => step.duration).filter(Boolean)
    const ns = durations.reduce(
      (sum, duration) =>
        sum + BigInt(duration.seconds) * 1000000000n + BigInt(duration.nanos),
      0n
    )
    record(
      pickles.get(cases.get(caseId)),
      status,
      durations.length ? Number(ns) / 1e6 : undefined
    )
  }
}

function verify(actual, expected, label) {
  for (const field of ['passed', 'failed', 'other', 'untimed']) {
    assert.equal(actual[field] ?? 0, expected[field], `${label}: ${field}`)
  }
  if (expected.durationMs === undefined) {
    assert.equal(actual.durationMs, undefined, `${label}: untimed`)
  } else {
    assert.ok(
      Math.abs(actual.durationMs - expected.durationMs) < 1e-6,
      `${label}: ${actual.durationMs} ms != ${expected.durationMs} ms`
    )
  }
}

assert.equal(fragment.files.length, files.size)
const total = emptyFigures()
for (const file of fragment.files) {
  const expected = files.get(file.path)
  assert.ok(expected, file.path)
  verify(file, expected, file.path)
  for (const field of ['passed', 'failed', 'other', 'untimed'])
    total[field] += expected[field]
  if (expected.durationMs !== undefined) {
    total.durationMs = (total.durationMs ?? 0) + expected.durationMs
  }
}
verify(fragment, total, fragment.entry)
const example = [...files].find(([, figures]) => figures.durationMs > 0)
console.log(
  `CI_PROOF_RUNNER_AUDIT ${JSON.stringify({
    entry: fragment.entry,
    part: fragment.part,
    fileCount: files.size,
    total,
    example,
  })}`
)
