import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  deployRun,
  releaseJobs,
  runBootstrapCommand,
} from './application-release-bootstrap-fixtures.mjs'

const selectedCiSha = 'b'.repeat(40)
const admissionLog = (overrides = {}) => {
  const release = {
    tag: 'v1.2.3',
    ref: 'refs/tags/v1.2.3',
    refOid: 'a'.repeat(40),
    sha: selectedCiSha,
    ...overrides,
  }
  return [
    `2026-09-07T01:02:03Z ${JSON.stringify(release)}`,
    `2026-09-07T01:03:04Z ${JSON.stringify({ state: 'ready', sha: selectedCiSha, runId: 42, runAttempt: 3 })}`,
  ].join('\n')
}

test('classifies a successful application publication from its step and admission log', async (t) => {
  const result = await runBootstrapCommand(t, {
    tags: ['v1.2.3'],
    runs: [deployRun()],
    jobs: { 91: releaseJobs() },
    logs: { 501: admissionLog() },
  })

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), {
    state: 'published',
    tag: 'v1.2.3',
    refOid: 'a'.repeat(40),
    sha: selectedCiSha,
    runId: 42,
    runAttempt: 3,
  })
  assert.deepEqual(Object.fromEntries(result.requests[0].searchParams), {
    event: 'push',
    per_page: '100',
    page: '1',
  })
  assert.deepEqual(
    result.requests.map((request) => request.pathname),
    [
      '/repos/nerds-odd-e/doughnut/actions/workflows/deploy.yml/runs',
      '/repos/nerds-odd-e/doughnut/actions/runs/91/jobs',
      '/repos/nerds-odd-e/doughnut/actions/jobs/501/logs',
    ]
  )
})

test('classifies an installation with no application tags or deploy runs as empty', async (t) => {
  const result = await runBootstrapCommand(t, {})

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), { state: 'empty' })
  assert.equal(result.requests.length, 1)
})

for (const [scenario, jobs] of [
  [
    'CI-only success',
    [
      {
        id: 501,
        run_attempt: 2,
        name: 'Admit selected application release',
      },
    ],
  ],
  [
    'CLI-only success',
    [
      {
        id: 502,
        run_attempt: 2,
        name: 'GCP deploy (GCS + MIG + health probe)',
        steps: [{ name: 'Upload CLI bundle', conclusion: 'success' }],
      },
    ],
  ],
  ['whole-workflow success without publication', releaseJobs('skipped')],
  ['unsuccessful publication', releaseJobs('failure')],
]) {
  test(`${scenario} does not qualify as an existing application publication`, async (t) => {
    const result = await runBootstrapCommand(t, {
      tags: ['v1.2.3'],
      runs: [deployRun()],
      jobs: { 91: jobs },
    })

    assert.equal(result.status, 1)
    assert.equal(JSON.parse(result.stdout).state, 'ambiguous')
    assert.match(
      result.stderr,
      /does not verify a successful application publication/
    )
  })
}

test('a successful publication with missing admission logs is ambiguous', async (t) => {
  const result = await runBootstrapCommand(t, {
    tags: ['v1.2.3'],
    runs: [deployRun()],
    jobs: { 91: releaseJobs() },
  })

  assert.equal(result.status, 1)
  assert.equal(JSON.parse(result.stdout).state, 'ambiguous')
  assert.match(result.stderr, /Admission log lookup.*HTTP 404/)
})

test('a successful publication with unparsable admission evidence is ambiguous', async (t) => {
  const result = await runBootstrapCommand(t, {
    tags: ['v1.2.3'],
    runs: [deployRun()],
    jobs: { 91: releaseJobs() },
    logs: { 501: 'not JSON\n{incomplete' },
  })

  assert.equal(result.status, 1)
  assert.equal(JSON.parse(result.stdout).state, 'ambiguous')
  assert.match(result.stderr, /unverifiable admission evidence/)
})

test('an incomplete deploy run is ambiguous without inspecting its jobs', async (t) => {
  const result = await runBootstrapCommand(t, {
    tags: ['v1.2.3'],
    runs: [deployRun({ status: 'in_progress', conclusion: null })],
  })

  assert.equal(result.status, 1)
  assert.equal(JSON.parse(result.stdout).state, 'ambiguous')
  assert.match(result.stderr, /contains an incomplete run/)
  assert.equal(result.requests.length, 1)
})
