import assert from 'node:assert/strict'
import { existsSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { test } from 'node:test'
import { waitForSelectedCi } from './application-release-ci.mjs'
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
  const identity = fixture.run({
    ref: 'refs/tags/v1.2.3',
    after: fixture.git('rev-parse', 'refs/tags/v1.2.3'),
  })
  assert.equal(identity.status, 0, identity.stderr)

  const result = await runStateCommand(t, {
    args: ['--check-release'],
    existingBody: JSON.stringify(publishingRecord(persistedRelease)),
    release: JSON.parse(identity.stdout),
    repository: fixture.repository,
  })

  assert.equal(result.status, 1)
  assert.match(result.stderr, /release identity mismatch/i)
  assert.equal(result.requests.length, 1)
  assert.equal(result.requests[0].method, 'GET')
  assert.deepEqual(result.uploads, [])
}

for (const ordering of ['tag-first', 'CI-first']) {
  test(`${ordering} release entry publishes the exact tagged main payload`, async (t) => {
    const fixture = makePublication(t, 'forced')
    const identity = fixture.fixture.run({
      ref: 'refs/tags/v1.2.3',
      after: fixture.refOid,
    })
    assert.equal(identity.status, 0, identity.stderr)
    const release = JSON.parse(identity.stdout)
    const queries = []
    const responses =
      ordering === 'tag-first'
        ? [
            [],
            [ciRun({ head_sha: release.sha, status: 'in_progress' })],
            [ciRun({ head_sha: release.sha })],
          ]
        : [[ciRun({ head_sha: release.sha })]]
    t.mock.method(globalThis, 'fetch', async (url) => {
      queries.push(url)
      const runs = responses.shift()
      return {
        ok: true,
        json: async () => ({ total_count: runs.length, workflow_runs: runs }),
      }
    })
    let pauses = 0
    const timers = new Set()
    const clock = {
      setTimeout(callback, delay) {
        const timer = { callback, delay }
        timers.add(timer)
        if (delay === 30_000) {
          pauses++
          queueMicrotask(() => {
            timers.delete(timer)
            callback()
          })
        }
        return timer
      },
      clearTimeout(timer) {
        timers.delete(timer)
      },
    }
    const admitted = await waitForSelectedCi({
      repository,
      sha: release.sha,
      clock,
    })
    const publication = fixture.publish(release, admitted)
    assert.equal(publication.status, 0, publication.stderr)
    assert.equal(pauses, ordering === 'tag-first' ? 2 : 0)
    assert.equal(timers.size, 0)
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
}

test('a completed release replay succeeds after artifacts expire without any production write', async (t) => {
  const fixture = makePublication(t, 'forced')
  const identity = fixture.fixture.run({
    ref: 'refs/tags/v1.2.3',
    after: fixture.refOid,
  })
  assert.equal(identity.status, 0, identity.stderr)
  const release = JSON.parse(identity.stdout)
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
  const original = fixture.run({
    ref: 'refs/tags/v1.2.3',
    after: fixture.git('rev-parse', 'refs/tags/v1.2.3'),
  })
  assert.equal(original.status, 0, original.stderr)
  const persistedRelease = JSON.parse(original.stdout)
  const replacement = fixture.commit('Replacement release')
  fixture.git('tag', '-f', 'v1.2.3', replacement)

  await assertPersistedIdentityRejects(t, fixture, persistedRelease)
})

test('an annotated tag object replacement is rejected when its commit is unchanged', async (t) => {
  const fixture = makePublication(t, 'forced').fixture
  fixture.git('tag', '-d', 'v1.2.3')
  const originalRefOid = fixture.tag('v1.2.3', true, fixture.sha)
  const original = fixture.run({
    ref: 'refs/tags/v1.2.3',
    after: originalRefOid,
  })
  assert.equal(original.status, 0, original.stderr)
  const persistedRelease = JSON.parse(original.stdout)
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
