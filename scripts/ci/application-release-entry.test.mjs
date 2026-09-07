import assert from 'node:assert/strict'
import { existsSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { test } from 'node:test'
import { querySelectedCi } from './application-release-ci.mjs'
import { ciRun, repository } from './application-release-ci-fixtures.mjs'
import {
  hash,
  makePublication,
} from './application-release-publication-fixtures.mjs'
import { runStateCommand } from './application-release-state-fixtures.mjs'

const publishingRecord = (release) => ({
  tag: release.tag,
  ref_oid: release.refOid,
  sha: release.sha,
  ci_run_id: '42',
  ci_run_attempt: '3',
  outcome: 'publishing',
})

async function assertPersistedIdentityRejects(t, fixture, persistedRelease) {
  const result = await runStateCommand(t, {
    args: ['--check-release'],
    existingBody: JSON.stringify(publishingRecord(persistedRelease)),
    release: fixture.release(),
    repository: fixture.repository,
  })

  assert.equal(result.status, 1)
  assert.match(result.stderr, /release identity mismatch/i)
  assert.equal(result.requests.length, 1)
  assert.equal(result.requests[0].method, 'GET')
  assert.deepEqual(result.uploads, [])
}

test('ready release entry publishes the exact tagged main payload', async (t) => {
  const fixture = makePublication(t, 'forced')
  const release = fixture.fixture.release()
  const queries = []
  t.mock.method(globalThis, 'fetch', async (url) => {
    queries.push(url)
    return {
      ok: true,
      json: async () => ({
        total_count: 1,
        workflow_runs: [ciRun({ head_sha: release.sha })],
      }),
    }
  })
  const admitted = await querySelectedCi({
    repository,
    sha: release.sha,
  })
  const publication = fixture.publish(release, admitted)
  assert.equal(publication.status, 0, publication.stderr)
  assert.ok(
    queries.every((url) => url.searchParams.get('head_sha') === fixture.sha)
  )
  const trace = readFileSync(fixture.trace, 'utf8')
  assert.match(trace, new RegExp(`frontend/${fixture.sha}/`))
  assert.match(trace, /artifacts-42\/bundle.mjs/)
  assert.match(trace, /artifacts-42\/donut.jar/)
  assert.doesNotMatch(trace, /artifacts-99/)
  assert.equal(
    readFileSync(`${fixture.root}/captured-spa`, 'utf8'),
    'selected SPA'
  )
  assert.equal(
    readFileSync(`${fixture.root}/captured-cli`, 'utf8'),
    'selected CLI'
  )
  const record = JSON.parse(readFileSync(`${fixture.root}/saved-record`))
  assert.equal(record.git_sha, fixture.sha)
  assert.equal(record.sha256, hash('selected jar'))
})

test('a completed release replay succeeds after artifacts expire without any production write', async (t) => {
  const fixture = makePublication(t, 'forced')
  const release = fixture.fixture.release()
  const record = {
    tag: release.tag,
    ref_oid: release.refOid,
    sha: release.sha,
    ci_run_id: '42',
    ci_run_attempt: '3',
    outcome: 'succeeded',
  }
  rmSync(`${fixture.root}/artifacts-42`, { recursive: true })
  writeFileSync(`${fixture.root}/captured-cli`, 'independent CLI')

  const replay = await runStateCommand(t, {
    args: ['--check-release'],
    existingBody: `${JSON.stringify(record)}\n`,
    release,
    repository: fixture.root,
  })

  assert.equal(replay.status, 0, replay.stderr)
  assert.deepEqual(JSON.parse(replay.stdout), { state: 'already-released' })
  assert.equal(replay.output, 'state=already-released\n')
  assert.equal(replay.requests.length, 1)
  assert.match(replay.requests[0].url.pathname, /^\/storage\/v1\//)
  assert.deepEqual(replay.uploads, [])
  assert.equal(existsSync(fixture.trace), false)
  assert.equal(existsSync(fixture.applicationRecords), false)
  assert.equal(existsSync(`${fixture.root}/captured-spa`), false)
  assert.equal(
    readFileSync(`${fixture.root}/captured-cli`, 'utf8'),
    'independent CLI'
  )
  assert.equal(existsSync(`${fixture.root}/saved-record`), false)
  assert.equal(existsSync(`${fixture.root}/captured-map`), false)
})

test('a tag moved to another commit is rejected against its persisted failed identity', async (t) => {
  const fixture = makePublication(t, 'forced').fixture
  const persistedRelease = fixture.release()
  const replacement = fixture.commit('Replacement release')
  fixture.git('tag', '-f', 'v1.2.3', replacement)

  await assertPersistedIdentityRejects(t, fixture, persistedRelease)
})

test('an annotated tag object replacement is rejected when its commit is unchanged', async (t) => {
  const fixture = makePublication(t, 'forced').fixture
  fixture.git('tag', '-d', 'v1.2.3')
  fixture.tag('v1.2.3', true, fixture.sha)
  const persistedRelease = fixture.release()
  fixture.git(
    'tag',
    '-f',
    '-a',
    '-m',
    'replacement annotation',
    'v1.2.3',
    fixture.sha
  )
  assert.equal(
    fixture.git('rev-parse', 'refs/tags/v1.2.3^{}'),
    persistedRelease.sha
  )

  await assertPersistedIdentityRejects(t, fixture, persistedRelease)
})
