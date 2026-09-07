import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  ciRun,
  repository,
  runCiCommand,
  selectedSha,
} from './application-release-ci-fixtures.mjs'

const page = (...runs) => ({ total_count: runs.length, workflow_runs: runs })

test('admits successful exact-commit CI and queries the specific workflow without hiding failures', async (t) => {
  const result = await runCiCommand(t, [page(ciRun())])
  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), {
    state: 'ready',
    sha: selectedSha,
    runId: 42,
    runAttempt: 1,
  })
  assert.equal(
    result.requests[0].pathname,
    `/repos/${repository}/actions/workflows/ci.yml/runs`
  )
  assert.deepEqual(Object.fromEntries(result.requests[0].searchParams), {
    branch: 'main',
    event: 'push',
    head_sha: selectedSha,
    per_page: '100',
    page: '1',
  })
})

for (const [field, value] of Object.entries({
  repository: { full_name: 'someone/else' },
  head_repository: { full_name: 'someone/else' },
  path: '.github/workflows/other.yml',
  head_branch: 'feature',
  event: 'pull_request',
  head_sha: 'b'.repeat(40),
})) {
  test(`does not borrow successful CI with a different ${field}`, async (t) => {
    const result = await runCiCommand(t, [page(ciRun({ [field]: value }))])
    assert.equal(JSON.parse(result.stdout).state, 'pending')
  })
}

test('admits the CI workflow path qualified with its Git ref', async (t) => {
  const result = await runCiCommand(t, [
    page(ciRun({ path: '.github/workflows/ci.yml@main' })),
  ])
  assert.equal(JSON.parse(result.stdout).state, 'ready')
})

test('missing CI remains pending', async (t) => {
  const result = await runCiCommand(t, [page()])
  assert.equal(JSON.parse(result.stdout).state, 'pending')
})

test('a newer unfinished run supersedes older green CI', async (t) => {
  const result = await runCiCommand(t, [
    page(ciRun(), ciRun({ id: 43, run_number: 13, status: 'in_progress' })),
  ])
  assert.deepEqual(JSON.parse(result.stdout), {
    state: 'pending',
    sha: selectedSha,
    runId: 43,
    runAttempt: 1,
  })
})

for (const conclusion of [
  'failure',
  'cancelled',
  'timed_out',
  'skipped',
  'neutral',
]) {
  test(`latest ${conclusion} cannot fall back to an older green run`, async (t) => {
    const result = await runCiCommand(t, [
      page(ciRun(), ciRun({ id: 43, run_number: 13, conclusion })),
    ])
    assert.equal(result.status, 1)
    assert.match(result.stderr, new RegExp(`CI 43 attempt 1.*${conclusion}`))
  })
}

test('a newer attempt cannot borrow its earlier successful attempt', async (t) => {
  const result = await runCiCommand(t, [
    page(ciRun(), ciRun({ run_attempt: 2, conclusion: 'failure' })),
  ])
  assert.match(result.stderr, /CI 42 attempt 2.*failure/)
})

test('examines later pages before selecting the latest run', async (t) => {
  const result = await runCiCommand(t, [
    {
      total_count: 101,
      workflow_runs: Array.from({ length: 100 }, () => ciRun()),
    },
    {
      total_count: 101,
      workflow_runs: [ciRun({ run_number: 13, conclusion: 'failure' })],
    },
  ])
  assert.equal(result.requests.length, 2)
  assert.match(result.stderr, /finished with failure/)
})

test('refuses an incomplete search above the GitHub result cap', async (t) => {
  const result = await runCiCommand(t, [
    { total_count: 1001, workflow_runs: [ciRun()] },
  ])
  assert.equal(result.requests.length, 1)
  assert.match(result.stderr, /1000-run search limit/)
})

test('surfaces GitHub transport failure instead of admitting a release', async (t) => {
  const result = await runCiCommand(t, [{ status: 503 }])
  assert.equal(result.status, 1)
  assert.match(result.stderr, /CI lookup failed: HTTP 503/)
})
