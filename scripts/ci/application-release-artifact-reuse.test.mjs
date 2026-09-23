import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makeReleaseRepository } from './application-release-fixtures.mjs'
import { ciRun } from './application-release-ci-fixtures.mjs'
import { runReconciliationCommand as reconcile } from './application-release-reconciliation-fixtures.mjs'

function documentationRelease(t) {
  const fixture = makeReleaseRepository(t)
  fixture.write(
    '.github/workflows/ci.yml',
    'on:\n  push:\n    branches: ["**"]\n    paths-ignore: ["docs/**", ".planning/**"]\n'
  )
  fixture.write('app.txt', 'tested application')
  const ciSha = fixture.commit('Tested application and CI policy')
  fixture.write('docs/guide.md', 'documentation')
  fixture.commit('First skipped documentation revision')
  fixture.write('.planning/story.md', 'plan')
  const sha = fixture.commit('Documentation only')
  return { fixture, ciSha, sha }
}

test('documentation-only tag reuses nearest tested ancestor artifacts with separate provenance', async (t) => {
  const { fixture, ciSha, sha } = documentationRelease(t)
  fixture.tag('v1.2.3', false, sha)
  fixture.clone({ fullHistory: true })
  const result = await reconcile(t, fixture, 'refs/tags/v1.2.3', {
    [ciSha]: [ciRun({ head_sha: ciSha })],
  })
  assert.equal(result.status, 0, result.stderr)
  const selected = JSON.parse(result.stdout)
  assert.deepEqual(
    [selected.state, selected.sha, selected.ciSha, selected.runId],
    ['ready', sha, ciSha, 42]
  )
})

for (const change of [
  'application edit',
  'application deletion',
  'rename application into docs',
]) {
  test(`ignored-only reuse rejects ${change}`, async (t) => {
    const { fixture, ciSha } = documentationRelease(t)
    if (change === 'application edit') fixture.write('app.txt', 'untested')
    else if (change === 'application deletion') fixture.git('rm', 'app.txt')
    else fixture.git('mv', 'app.txt', 'docs/app.txt')
    const sha = fixture.commit(change)
    fixture.tag('v1.2.3', false, sha)
    fixture.clone({ fullHistory: true })
    const result = await reconcile(t, fixture, 'refs/tags/v1.2.3', {
      [ciSha]: [ciRun({ head_sha: ciSha })],
    })
    assert.equal(JSON.parse(result.stdout).state, 'waiting', result.stderr)
  })
}

for (const exact of [true, false]) {
  for (const status of ['in_progress', 'failure']) {
    test(`${exact ? 'exact' : 'nearest ancestor'} ${status} cannot borrow an older green run`, async (t) => {
      const { fixture, ciSha, sha } = documentationRelease(t)
      const latestSha = exact ? sha : ciSha
      fixture.tag('v1.2.3', false, sha)
      fixture.clone({ fullHistory: true })
      const result = await reconcile(t, fixture, 'refs/tags/v1.2.3', {
        [ciSha]: [ciRun({ head_sha: ciSha })],
        [latestSha]: [
          ciRun({ head_sha: latestSha }),
          ciRun({
            head_sha: latestSha,
            run_attempt: 2,
            status: status === 'in_progress' ? status : 'completed',
            conclusion: status === 'failure' ? status : null,
          }),
        ],
      })
      const selected = JSON.parse(result.stdout)
      assert.deepEqual(
        [selected.state, selected.runAttempt, selected.ciSha],
        [status === 'failure' ? 'blocked' : 'waiting', 2, latestSha]
      )
    })
  }
}

test('equivalent non-ancestor successful CI cannot supply a release', async (t) => {
  const { fixture, ciSha, sha } = documentationRelease(t)
  fixture.git('checkout', '-b', 'unmerged', ciSha)
  const unrelated = fixture.commit('Unmerged test result')
  fixture.git('checkout', 'main')
  fixture.tag('v1.2.3', false, sha)
  fixture.clone({ fullHistory: true })
  const result = await reconcile(t, fixture, 'refs/tags/v1.2.3', {
    [unrelated]: [ciRun({ head_sha: unrelated })],
  })
  assert.equal(JSON.parse(result.stdout).state, 'waiting', result.stderr)
})

test('release equivalence uses the tagged path policy even when main later broadens it', async (t) => {
  const { fixture, ciSha } = documentationRelease(t)
  fixture.write('application/module.txt', 'untested application change')
  const sha = fixture.commit('Application change')
  fixture.tag('v1.2.3', false, sha)
  fixture.write(
    '.github/workflows/ci.yml',
    'on:\n  push:\n    paths-ignore: ["docs/**", ".planning/**", "application/**"]\n'
  )
  fixture.commit('Broaden current main policy')
  fixture.clone({ fullHistory: true })
  const result = await reconcile(t, fixture, 'refs/tags/v1.2.3', {
    [ciSha]: [ciRun({ head_sha: ciSha })],
  })
  assert.equal(JSON.parse(result.stdout).state, 'waiting', result.stderr)
})
