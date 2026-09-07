import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { querySelectedCi } from './application-release-ci.mjs'
import { ciRun, repository } from './application-release-ci-fixtures.mjs'
import { makeReleaseRepository } from './application-release-fixtures.mjs'
import { runPayloadAdmission } from './application-release-payload-fixtures.mjs'
import {
  hash,
  makePublication,
  readApplicationRecords,
} from './application-release-publication-fixtures.mjs'
import { runReconciliationCommand as reconcile } from './application-release-reconciliation-fixtures.mjs'
import { runStateCommand } from './application-release-state-fixtures.mjs'

async function selectFreshSuccessfulCiAttempt(t, sha) {
  const queries = []
  t.mock.method(globalThis, 'fetch', async (url) => {
    queries.push(url)
    return {
      ok: true,
      json: async () => ({
        total_count: 1,
        workflow_runs: [
          ciRun({
            id: 99,
            run_number: 13,
            run_attempt: 4,
            head_sha: sha,
          }),
        ],
      }),
    }
  })
  const ci = await querySelectedCi({ repository, sha })
  return { ci, queries }
}

test('an interrupted release retries the same identity with freshly selected CI artifacts', async (t) => {
  const fixture = makePublication(t, 'forced')
  const release = fixture.fixture.release()

  const interrupted = fixture.publish(
    release,
    { runId: 42, runAttempt: 3 },
    { failGsutilMatch: 'doughnut-cli-latest' }
  )

  assert.notEqual(interrupted.status, 0)
  assert.equal(
    readFileSync(`${fixture.root}/captured-spa`, 'utf8'),
    'selected SPA'
  )
  const interruptedRecord = JSON.parse(
    readFileSync(fixture.applicationRecords, 'utf8').trim()
  )
  assert.equal(interruptedRecord.outcome, 'publishing')

  const admission = await runStateCommand(t, {
    args: ['--check-release'],
    existingBody: JSON.stringify(interruptedRecord),
    release,
    repository: fixture.root,
  })

  assert.equal(admission.status, 0, admission.stderr)
  assert.deepEqual(JSON.parse(admission.stdout), { state: 'retry' })
  assert.equal(admission.output, 'state=retry\n')
  const { ci: freshCi, queries } = await selectFreshSuccessfulCiAttempt(
    t,
    release.sha
  )

  assert.deepEqual(freshCi, {
    state: 'ready',
    sha: release.sha,
    runId: 99,
    runAttempt: 4,
  })
  assert.ok(
    queries.every((url) => url.searchParams.get('head_sha') === release.sha)
  )
  const retried = fixture.publish(release, freshCi)
  assert.equal(retried.status, 0, retried.stderr)
  assert.equal(
    readFileSync(`${fixture.root}/captured-spa`, 'utf8'),
    'fresh SPA'
  )
  assert.equal(
    readFileSync(`${fixture.root}/captured-cli`, 'utf8'),
    'fresh CLI'
  )
  assert.equal(
    JSON.parse(readFileSync(`${fixture.root}/saved-record`, 'utf8')).sha256,
    hash('fresh jar')
  )
  const records = readApplicationRecords(fixture.applicationRecords)
  assert.deepEqual(
    records.map(({ ci_run_id, ci_run_attempt, outcome }) => ({
      ci_run_id,
      ci_run_attempt,
      outcome,
    })),
    [
      { ci_run_id: '42', ci_run_attempt: '3', outcome: 'publishing' },
      { ci_run_id: '99', ci_run_attempt: '4', outcome: 'publishing' },
      { ci_run_id: '99', ci_run_attempt: '4', outcome: 'succeeded' },
    ]
  )
})

test('a selected release resumes with a newer exact-identity CI attempt after artifacts expire', async (t) => {
  const fixture = makeReleaseRepository(t)
  const refOid = fixture.tag('v1.2.3', true)
  const release = fixture.release()
  fixture.clone()
  assert.notEqual(refOid, release.sha)

  const ready = await reconcile(t, fixture, release.ref, {
    [release.sha]: [
      ciRun({
        head_sha: release.sha,
        run_attempt: 3,
      }),
    ],
  })
  assert.equal(ready.status, 0, ready.stderr)
  assert.deepEqual(JSON.parse(ready.stdout), {
    state: 'ready',
    ...release,
    runId: 42,
    runAttempt: 3,
  })
  assert.deepEqual(ready.uploads, [
    {
      tag: release.tag,
      ref_oid: refOid,
      sha: release.sha,
      outcome: 'selected',
    },
  ])
  const selectedRecord = ready.uploads[0]

  const productionWrites = `${fixture.repository}/production-writes`
  const missingArtifacts = `${fixture.repository}/release-artifacts`

  const unavailable = runPayloadAdmission({
    backend: `${missingArtifacts}/backend/donut.jar`,
    frontend: `${missingArtifacts}/frontend`,
    cli: `${missingArtifacts}/cli/donut-cli.bundle.mjs`,
    publicationTrace: productionWrites,
  })

  assert.equal(unavailable.status, 1)
  assert.equal(unavailable.publicationReached, false)

  const recovered = await reconcile(
    t,
    fixture,
    'refs/heads/main',
    {
      [release.sha]: [
        ciRun({
          id: 99,
          run_number: 13,
          run_attempt: 4,
          head_sha: release.sha,
        }),
      ],
    },
    selectedRecord
  )

  assert.equal(recovered.status, 0, recovered.stderr)
  assert.deepEqual(JSON.parse(recovered.stdout), {
    state: 'ready',
    ...release,
    runId: 99,
    runAttempt: 4,
  })
  assert.deepEqual(recovered.uploads, [])
  assert.deepEqual(
    recovered.requests
      .filter((request) => request.url.searchParams.has('head_sha'))
      .map((request) => request.url.searchParams.get('head_sha')),
    [release.sha]
  )
})
