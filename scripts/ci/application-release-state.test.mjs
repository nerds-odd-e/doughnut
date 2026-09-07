import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  deployRun,
  releaseJobs,
} from './application-release-bootstrap-fixtures.mjs'
import {
  runStateCommand,
  runStateInitialization,
} from './application-release-state-fixtures.mjs'

const selectedCiSha = 'b'.repeat(40)
const publishedRecord = (outcome) => ({
  tag: 'v1.2.3',
  ref_oid: 'a'.repeat(40),
  sha: selectedCiSha,
  ci_run_id: '42',
  ci_run_attempt: '3',
  outcome,
})
const admissionLog = [
  JSON.stringify({
    tag: 'v1.2.3',
    ref: 'refs/tags/v1.2.3',
    refOid: 'a'.repeat(40),
    sha: selectedCiSha,
  }),
  JSON.stringify({
    state: 'ready',
    sha: selectedCiSha,
    runId: 42,
    runAttempt: 3,
  }),
].join('\n')

for (const record of [
  publishedRecord('publishing'),
  publishedRecord('succeeded'),
  { outcome: 'initialized-empty' },
]) {
  test(`valid existing ${record.outcome} state is preserved without a write`, async (t) => {
    const existingBody = `${JSON.stringify(record, null, 2)}\n`
    const result = await runStateInitialization(t, { existingBody })

    assert.equal(result.status, 0, result.stderr)
    assert.deepEqual(JSON.parse(result.stdout), { state: 'existing', record })
    assert.equal(result.requests.length, 1)
    assert.equal(result.requests[0].method, 'GET')
    assert.equal(result.requests[0].url.searchParams.get('alt'), 'media')
    assert.equal(result.requests[0].authorization, 'Bearer gcs-token')
    assert.deepEqual(result.uploads, [])
  })
}

for (const [scenario, existingBody, error] of [
  ['invalid JSON', '{', /returned invalid JSON/],
  [
    'invalid schema',
    JSON.stringify({ outcome: 'succeeded', sha: selectedCiSha }),
    /invalid schema/,
  ],
]) {
  test(`${scenario} existing state fails without a write`, async (t) => {
    const result = await runStateInitialization(t, { existingBody })

    assert.equal(result.status, 1)
    assert.match(result.stderr, error)
    assert.match(result.stderr, /Identify the published application release/)
    assert.equal(result.requests.length, 1)
    assert.deepEqual(result.uploads, [])
  })
}

test('absent state initializes the exact succeeded publication identity', async (t) => {
  const result = await runStateInitialization(t, {
    tags: ['v1.2.3'],
    runs: [deployRun()],
    jobs: { 91: releaseJobs() },
    logs: { 501: admissionLog },
  })

  assert.equal(result.status, 0, result.stderr)
  const record = publishedRecord('succeeded')
  assert.deepEqual(JSON.parse(result.stdout), { state: 'initialized', record })
  assert.deepEqual(result.uploads.map(JSON.parse), [record])
  const upload = result.requests.at(-1)
  assert.equal(upload.method, 'POST')
  assert.equal(upload.authorization, 'Bearer gcs-token')
  assert.deepEqual(Object.fromEntries(upload.url.searchParams), {
    uploadType: 'media',
    name: 'deploy/application-release.json',
    ifGenerationMatch: '0',
  })
})

test('absent state initializes verified empty tracking', async (t) => {
  const result = await runStateInitialization(t)

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(result.uploads.map(JSON.parse), [
    { outcome: 'initialized-empty' },
  ])
})

test('ambiguous publication evidence fails without a write', async (t) => {
  const result = await runStateInitialization(t, { tags: ['v1.2.3'] })

  assert.equal(result.status, 1)
  assert.match(result.stderr, /publication history is ambiguous/)
  assert.match(result.stderr, /Identify the published application release/)
  assert.deepEqual(result.uploads, [])
})

test('a non-404 state read fails without inspecting history or writing', async (t) => {
  const result = await runStateInitialization(t, { readStatus: 403 })

  assert.equal(result.status, 1)
  assert.match(result.stderr, /state read failed: HTTP 403/)
  assert.equal(result.requests.length, 1)
  assert.deepEqual(result.uploads, [])
})

test('a state transport failure fails loudly without a write', async (t) => {
  const result = await runStateInitialization(t, { unavailable: true })

  assert.equal(result.status, 1)
  assert.match(result.stderr, /GCS request failed/)
  assert.match(result.stderr, /Identify the published application release/)
  assert.deepEqual(result.uploads, [])
})

test('the verified current deploy run and its application tag are ignored', async (t) => {
  const result = await runStateInitialization(t, {
    tags: ['v1.2.3'],
    runs: [deployRun({ status: 'in_progress', conclusion: null })],
    currentRunId: '91',
    currentRef: 'refs/tags/v1.2.3',
  })

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(result.uploads.map(JSON.parse), [
    { outcome: 'initialized-empty' },
  ])
})

test('an unrelated incomplete deploy run remains ambiguous', async (t) => {
  const current = deployRun({ status: 'in_progress', conclusion: null })
  const unrelated = deployRun({
    id: 92,
    run_number: 18,
    status: 'queued',
    conclusion: null,
  })
  const result = await runStateInitialization(t, {
    tags: ['v1.2.3'],
    runs: [current, unrelated],
    currentRunId: '91',
    currentRef: 'refs/tags/v1.2.3',
  })

  assert.equal(result.status, 1)
  assert.match(result.stderr, /contains an incomplete run/)
  assert.deepEqual(result.uploads, [])
})

test('a create-only conflict fails without replacing state', async (t) => {
  const result = await runStateInitialization(t, { createStatus: 412 })

  assert.equal(result.status, 1)
  assert.match(result.stderr, /state create failed: HTTP 412/)
  assert.equal(result.uploads.length, 1)
  assert.equal(
    result.requests.at(-1).url.searchParams.get('ifGenerationMatch'),
    '0'
  )
})

test('the exact succeeded release is reported as already released without changing state', async (t) => {
  const record = publishedRecord('succeeded')
  const result = await runStateCommand(t, {
    args: ['--check-release'],
    existingBody: JSON.stringify(record),
    release: {
      tag: record.tag,
      refOid: record.ref_oid,
      sha: record.sha,
    },
  })

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), { state: 'already-released' })
  assert.equal(result.output, 'state=already-released\n')
  assert.equal(result.requests.length, 1)
  assert.equal(result.requests[0].method, 'GET')
  assert.deepEqual(result.uploads, [])
})

test('a different release tag continues through existing admission', async (t) => {
  const record = publishedRecord('succeeded')
  const result = await runStateCommand(t, {
    args: ['--check-release'],
    existingBody: JSON.stringify(record),
    release: {
      tag: 'v1.2.4',
      refOid: 'c'.repeat(40),
      sha: record.sha,
    },
  })

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), { state: 'continue' })
  assert.deepEqual(result.uploads, [])
})
