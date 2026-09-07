import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { waitForSelectedCi } from './application-release-ci.mjs'
import { ciRun, repository } from './application-release-ci-fixtures.mjs'
import {
  hash,
  makePublication,
} from './application-release-publication-fixtures.mjs'

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
