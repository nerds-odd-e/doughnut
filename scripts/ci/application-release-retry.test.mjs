import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { waitForSelectedCi } from './application-release-ci.mjs'
import { ciRun, repository } from './application-release-ci-fixtures.mjs'
import {
  hash,
  makePublication,
  readApplicationRecords,
} from './application-release-publication-fixtures.mjs'
import { runStateCommand } from './application-release-state-fixtures.mjs'

test('an interrupted release retries the same identity with freshly selected CI artifacts', async (t) => {
  const fixture = makePublication(t, 'forced')
  const identity = fixture.fixture.run({
    ref: 'refs/tags/v1.2.3',
    after: fixture.refOid,
  })
  assert.equal(identity.status, 0, identity.stderr)
  const release = JSON.parse(identity.stdout)

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
            head_sha: release.sha,
          }),
        ],
      }),
    }
  })
  const freshCi = await waitForSelectedCi({
    repository,
    sha: release.sha,
  })

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
