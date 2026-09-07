import assert from 'node:assert/strict'
import { test } from 'node:test'
import { ciRun } from './application-release-ci-fixtures.mjs'
import { makeReleaseRepository } from './application-release-fixtures.mjs'
import { runReconciliationCommand as reconcile } from './application-release-reconciliation-fixtures.mjs'

test('reconciliation keeps the highest numeric pending version across reversed tag requests', async (t) => {
  const fixture = makeReleaseRepository(t)
  const lowerSha = fixture.sha
  fixture.tag('v1.3.9')
  const higherSha = fixture.commit('Higher pending release')
  const higherRefOid = fixture.tag('v1.3.10', false, higherSha)
  fixture.clone()
  const runsBySha = {
    [lowerSha]: [ciRun({ head_sha: lowerSha })],
    [higherSha]: [
      ciRun({
        id: 43,
        head_sha: higherSha,
        status: 'in_progress',
        conclusion: null,
      }),
    ],
  }

  for (const requestRef of ['refs/tags/v1.3.10', 'refs/tags/v1.3.9']) {
    const result = await reconcile(t, fixture, requestRef, runsBySha)
    assert.equal(result.status, 0, result.stderr)
    assert.deepEqual(JSON.parse(result.stdout), {
      state: 'waiting',
      tag: 'v1.3.10',
      ref: 'refs/tags/v1.3.10',
      refOid: higherRefOid,
      sha: higherSha,
      runId: 43,
      runAttempt: 1,
    })
    assert.equal(
      result.output,
      `state=waiting\ntag=v1.3.10\nref=refs/tags/v1.3.10\nrefOid=${higherRefOid}\nsha=${higherSha}\nrunId=43\nrunAttempt=1\n`
    )
    assert.deepEqual(
      result.requests
        .filter((request) => request.url.searchParams.has('head_sha'))
        .map((request) => request.url.searchParams.get('head_sha')),
      [higherSha]
    )
  }
})

test('premature tag returns waiting and an explicit retry after CI succeeds selects the same release', async (t) => {
  const fixture = makeReleaseRepository(t)
  const refOid = fixture.tag('v1.2.3', true)
  fixture.clone()

  const waiting = await reconcile(t, fixture, 'refs/tags/v1.2.3', {
    [fixture.sha]: [],
  })
  assert.equal(waiting.status, 0, waiting.stderr)
  assert.equal(JSON.parse(waiting.stdout).state, 'waiting')
  assert.equal(waiting.uploads.length, 1)

  const ready = await reconcile(
    t,
    fixture,
    'refs/tags/v1.2.3',
    { [fixture.sha]: [ciRun({ head_sha: fixture.sha })] },
    waiting.uploads[0]
  )
  assert.equal(ready.status, 0, ready.stderr)
  assert.deepEqual(JSON.parse(ready.stdout), {
    state: 'ready',
    tag: 'v1.2.3',
    ref: 'refs/tags/v1.2.3',
    refOid,
    sha: fixture.sha,
    runId: 42,
    runAttempt: 1,
  })
})

test('reconciliation ignores non-stable application tags', async (t) => {
  const fixture = makeReleaseRepository(t)
  for (const tag of [
    'v1.2',
    'v1.2.3-rc.1',
    'v1.2.3+build',
    'v01.2.3',
    'v1.02.3',
    'v1.2.03',
  ]) {
    fixture.tag(tag)
  }
  fixture.clone()

  const result = await reconcile(t, fixture, 'refs/heads/main', {})

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), { state: 'none' })
  assert.deepEqual(
    result.requests.filter((request) =>
      request.url.pathname.includes('/actions/')
    ),
    []
  )
})

test('reconciliation ignores a release commit outside main', async (t) => {
  const fixture = makeReleaseRepository(t)
  fixture.git('checkout', '-b', 'feature')
  const offMainSha = fixture.commit('Unmerged release')
  fixture.tag('v1.2.3', false, offMainSha)
  fixture.git('checkout', 'main')
  fixture.clone()

  const result = await reconcile(t, fixture, 'refs/tags/v1.2.3', {})

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), { state: 'none' })
  assert.deepEqual(
    result.requests.filter((request) =>
      request.url.pathname.includes('/actions/')
    ),
    []
  )
})

test('failed CI blocks the current highest release without failing or selecting an older tag', async (t) => {
  const fixture = makeReleaseRepository(t)
  const lowerSha = fixture.sha
  fixture.tag('v1.3.9')
  const higherSha = fixture.commit('Higher blocked release')
  fixture.tag('v1.3.10', false, higherSha)
  fixture.clone()

  const result = await reconcile(t, fixture, 'refs/heads/main', {
    [lowerSha]: [ciRun({ head_sha: lowerSha })],
    [higherSha]: [
      ciRun({
        id: 43,
        head_sha: higherSha,
        conclusion: 'failure',
      }),
    ],
  })

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), {
    state: 'blocked',
    tag: 'v1.3.10',
    ref: 'refs/tags/v1.3.10',
    refOid: fixture.git('rev-parse', 'refs/tags/v1.3.10'),
    sha: higherSha,
    runId: 43,
    runAttempt: 1,
    diagnostic: `CI 43 attempt 1 for ${higherSha} finished with failure`,
  })
  assert.deepEqual(
    result.requests
      .filter((request) => request.url.searchParams.has('head_sha'))
      .map((request) => request.url.searchParams.get('head_sha')),
    [higherSha]
  )
})
