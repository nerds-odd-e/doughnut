import assert from 'node:assert/strict'
import { test } from 'node:test'
import { ciRun } from './application-release-ci-fixtures.mjs'
import { makeReleaseRepository } from './application-release-fixtures.mjs'
import { runReconciliationCommand } from './application-release-reconciliation-fixtures.mjs'

const selectedRecord = (release) => ({
  tag: release.tag,
  ref_oid: release.refOid,
  sha: release.sha,
  outcome: 'selected',
})

for (const [scenario, runs, expectedState] of [
  ['absent CI', [], 'waiting'],
  [
    'in-progress CI',
    [ciRun({ status: 'in_progress', conclusion: null })],
    'waiting',
  ],
  ['failed CI', [ciRun({ conclusion: 'failure' })], 'blocked'],
  ['cancelled CI', [ciRun({ conclusion: 'cancelled' })], 'blocked'],
]) {
  test(`${scenario} freezes the highest numeric release before reporting ${expectedState}`, async (t) => {
    const fixture = makeReleaseRepository(t)
    fixture.tag('v1.3.9')
    const sha = fixture.commit('Higher release')
    fixture.tag('v1.3.10', true, sha)
    fixture.clone()
    const release = fixture.release('v1.3.10')

    const result = await runReconciliationCommand(
      t,
      fixture,
      'refs/tags/v1.3.9',
      { [sha]: runs.map((run) => ({ ...run, head_sha: sha })) }
    )

    assert.equal(result.status, 0, result.stderr)
    assert.equal(JSON.parse(result.stdout).state, expectedState)
    assert.deepEqual(result.uploads, [selectedRecord(release)])
  })
}

test('ready exact-SHA CI freezes the release before reporting ready', async (t) => {
  const fixture = makeReleaseRepository(t)
  fixture.tag('v1.2.3', true)
  fixture.clone()
  const release = fixture.release()

  const result = await runReconciliationCommand(
    t,
    fixture,
    'refs/tags/v1.2.3',
    { [release.sha]: [ciRun({ head_sha: release.sha })] }
  )

  assert.equal(result.status, 0, result.stderr)
  assert.equal(JSON.parse(result.stdout).state, 'ready')
  assert.deepEqual(result.uploads, [selectedRecord(release)])
})

for (const scenario of ['lightweight ref', 'annotated ref', 'peeled commit']) {
  test(`a replaced ${scenario} is rejected against its frozen identity`, async (t) => {
    const fixture = makeReleaseRepository(t)
    const annotated = scenario !== 'lightweight ref'
    fixture.tag('v1.2.3', annotated)
    fixture.clone()
    const persisted = fixture.release()
    const replacementSha =
      scenario === 'annotated ref'
        ? persisted.sha
        : fixture.commit('Replacement release')
    fixture.git(
      'tag',
      '-f',
      ...(annotated ? ['-a', '-m', 'replacement'] : []),
      'v1.2.3',
      replacementSha
    )

    const result = await runReconciliationCommand(
      t,
      fixture,
      'refs/heads/main',
      {},
      selectedRecord(persisted)
    )

    assert.equal(result.status, 1)
    assert.match(result.stderr, /release identity mismatch/i)
    assert.deepEqual(result.uploads, [])
    assert.equal(
      result.requests.some((request) =>
        request.url.pathname.includes('/actions/')
      ),
      false
    )
  })
}

test('a deleted frozen tag is rejected before CI or publication', async (t) => {
  const fixture = makeReleaseRepository(t)
  fixture.tag('v1.2.3')
  fixture.clone()
  const persisted = fixture.release()
  fixture.git('tag', '-d', 'v1.2.3')

  const result = await runReconciliationCommand(
    t,
    fixture,
    'refs/heads/main',
    {},
    selectedRecord(persisted)
  )

  assert.equal(result.status, 1)
  assert.match(result.stderr, /selected release tag v1\.2\.3 is missing/i)
  assert.deepEqual(result.uploads, [])
})

test('a higher waiting release replaces the selected numeric ceiling', async (t) => {
  const fixture = makeReleaseRepository(t)
  fixture.tag('v1.3.9')
  const lower = fixture.release('v1.3.9')
  const higherSha = fixture.commit('Higher release')
  fixture.tag('v1.3.10', false, higherSha)
  fixture.clone()
  const higher = fixture.release('v1.3.10')

  const result = await runReconciliationCommand(
    t,
    fixture,
    'refs/heads/main',
    { [higherSha]: [] },
    selectedRecord(lower)
  )

  assert.equal(result.status, 0, result.stderr)
  assert.equal(JSON.parse(result.stdout).state, 'waiting')
  assert.deepEqual(result.uploads, [selectedRecord(higher)])
})

test('a late lower release cannot replace a missing higher selected tag', async (t) => {
  const fixture = makeReleaseRepository(t)
  fixture.tag('v1.3.9')
  const lower = fixture.release('v1.3.9')
  fixture.clone()
  const missingHigher = {
    tag: 'v1.3.10',
    ref_oid: 'a'.repeat(40),
    sha: 'b'.repeat(40),
    outcome: 'selected',
  }

  const result = await runReconciliationCommand(
    t,
    fixture,
    'refs/tags/v1.3.9',
    { [lower.sha]: [ciRun({ head_sha: lower.sha })] },
    missingHigher
  )

  assert.equal(result.status, 0, result.stderr)
  assert.equal(JSON.parse(result.stdout).state, 'superseded')
  assert.deepEqual(result.uploads, [])
  assert.equal(
    result.requests.some((request) =>
      request.url.pathname.includes('/actions/')
    ),
    false
  )
})
