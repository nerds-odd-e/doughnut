import assert from 'node:assert/strict'
import { test } from 'node:test'
import { makeReleaseRepository } from './application-release-fixtures.mjs'

for (const annotated of [false, true]) {
  test(`selects the exact ${annotated ? 'annotated' : 'lightweight'} tag behind main from shallow history`, (t) => {
    const fixture = makeReleaseRepository(t)
    const after = fixture.tag('v1.2.3', annotated)
    fixture.commit('Main advances')
    fixture.clone()
    const result = fixture.run({ ref: 'refs/tags/v1.2.3', after })
    assert.equal(result.status, 0, result.stderr)
    assert.deepEqual(JSON.parse(result.stdout), {
      tag: 'v1.2.3',
      ref: 'refs/tags/v1.2.3',
      refOid: after,
      sha: fixture.sha,
    })
    assert.match(result.output, new RegExp(`sha=${fixture.sha}\\n`))
  })
}

for (const tag of [
  'v1.2',
  'v1.2.3-rc.1',
  'v1.2.3+build',
  'cli-v1.2.3',
  'v01.2.3',
  'v1.02.3',
  'v1.2.03',
]) {
  test(`rejects non-stable application tag ${tag}`, (t) => {
    const fixture = makeReleaseRepository(t)
    fixture.clone()
    const result = fixture.run({ ref: `refs/tags/${tag}`, after: fixture.sha })
    assert.equal(result.status, 1)
    assert.match(result.stderr, /stable vMAJOR.MINOR.PATCH/)
    assert.equal(result.output, '')
  })
}

for (const flag of ['forced', 'deleted']) {
  test(`rejects ${flag} release events`, (t) => {
    const fixture = makeReleaseRepository(t)
    fixture.clone()
    const result = fixture.run({
      ref: 'refs/tags/v1.2.3',
      after: fixture.sha,
      [flag]: true,
    })
    assert.match(result.stderr, /Deleted or force-updated/)
    assert.equal(result.status, 1)
  })
}

test('rejects a release commit outside main', (t) => {
  const fixture = makeReleaseRepository(t)
  fixture.git('checkout', '-b', 'feature')
  const after = fixture.commit('Unmerged feature')
  fixture.tag('v1.2.3', false, after)
  fixture.git('checkout', 'main')
  fixture.clone()
  const result = fixture.run({ ref: 'refs/tags/v1.2.3', after })
  assert.equal(result.status, 1)
  assert.match(result.stderr, /Release commit is not on main/)
})

test('rejects a tag that no longer identifies the event object', (t) => {
  const fixture = makeReleaseRepository(t)
  fixture.tag()
  const moved = fixture.commit('Replacement')
  fixture.git('tag', '-f', 'v1.2.3', moved)
  fixture.clone()
  const result = fixture.run({ ref: 'refs/tags/v1.2.3', after: fixture.sha })
  assert.equal(result.status, 1)
  assert.match(result.stderr, /Release ref changed or disappeared/)
})

test('rejects a release ref deleted since its event', (t) => {
  const fixture = makeReleaseRepository(t)
  const after = fixture.tag()
  fixture.clone()
  fixture.git('tag', '-d', 'v1.2.3')
  const result = fixture.run({ ref: 'refs/tags/v1.2.3', after })
  assert.equal(result.status, 1)
  assert.match(result.stderr, /Release ref changed or disappeared/)
})
