import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { existsSync, readFileSync, rmSync } from 'node:fs'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { waitForSelectedCi } from './application-release-ci.mjs'
import { ciRun, repository } from './application-release-ci-fixtures.mjs'
import {
  hash,
  makePublication,
  readApplicationRecords,
} from './application-release-publication-fixtures.mjs'
import { runStateCommand } from './application-release-state-fixtures.mjs'

const payloadCommand = fileURLToPath(
  new URL('./application-release-payload.mjs', import.meta.url)
)

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
  const ci = await waitForSelectedCi({ repository, sha })
  return { ci, queries }
}

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

test('missing artifacts stop before writes and a newer exact-SHA CI attempt resumes the same tag', async (t) => {
  const fixture = makePublication(t, 'forced')
  const identity = fixture.fixture.run({
    ref: 'refs/tags/v1.2.3',
    after: fixture.refOid,
  })
  assert.equal(identity.status, 0, identity.stderr)
  const release = JSON.parse(identity.stdout)
  rmSync(`${fixture.root}/artifacts-42`, { recursive: true })

  const unavailable = spawnSync(process.execPath, [payloadCommand], {
    encoding: 'utf8',
    env: {
      ...process.env,
      DEPLOY_JAR_PATH: fixture.jar,
      FRONTEND_STATIC_DIR: fixture.frontend,
      CLI_BUNDLE_SOURCE: fixture.cli,
    },
  })

  assert.equal(unavailable.status, 1)
  assert.equal(existsSync(fixture.trace), false)
  assert.equal(existsSync(fixture.applicationRecords), false)
  assert.equal(existsSync(`${fixture.root}/captured-spa`), false)
  assert.equal(existsSync(`${fixture.root}/captured-cli`), false)
  assert.equal(existsSync(`${fixture.root}/saved-record`), false)

  const { ci: freshCi } = await selectFreshSuccessfulCiAttempt(t, release.sha)
  const recovered = fixture.publish(release, freshCi)

  assert.equal(recovered.status, 0, recovered.stderr)
  assert.equal(freshCi.sha, release.sha)
  assert.equal(
    readFileSync(`${fixture.root}/captured-spa`, 'utf8'),
    'fresh SPA'
  )
  assert.deepEqual(
    readApplicationRecords(fixture.applicationRecords).map(
      ({ tag, sha, ci_run_id, ci_run_attempt, outcome }) => ({
        tag,
        sha,
        ci_run_id,
        ci_run_attempt,
        outcome,
      })
    ),
    [
      {
        tag: release.tag,
        sha: release.sha,
        ci_run_id: '99',
        ci_run_attempt: '4',
        outcome: 'publishing',
      },
      {
        tag: release.tag,
        sha: release.sha,
        ci_run_id: '99',
        ci_run_attempt: '4',
        outcome: 'succeeded',
      },
    ]
  )
})
