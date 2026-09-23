import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  makePublishedReleaseRecord as publishedRecord,
  runStateCommand,
} from './application-release-state-fixtures.mjs'

test('the exact succeeded release is reported as already released without changing state', async (t) => {
  const record = publishedRecord('succeeded')
  const result = await runStateCommand(t, {
    args: ['--check-release'],
    existingBody: JSON.stringify(record),
    release: {
      tag: record.tag,
      refOid: record.ref_oid,
      sha: record.sha,
    },
  })

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), { state: 'already-released' })
  assert.equal(result.output, 'state=already-released\n')
  assert.equal(result.requests.length, 1)
  assert.equal(result.requests[0].method, 'GET')
  assert.deepEqual(result.uploads, [])
})

test('a different release tag continues through existing admission', async (t) => {
  const record = publishedRecord('succeeded')
  const result = await runStateCommand(t, {
    args: ['--check-release'],
    existingBody: JSON.stringify(record),
    release: {
      tag: 'v1.2.4',
      refOid: 'c'.repeat(40),
      sha: record.sha,
    },
  })

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), { state: 'continue' })
  assert.deepEqual(result.uploads, [])
})

for (const outcome of ['publishing', 'succeeded']) {
  test(`a higher ${outcome} release supersedes an older candidate after its tag disappears`, async (t) => {
    const record = {
      ...publishedRecord(outcome),
      tag: 'v1.3.10',
    }
    const result = await runStateCommand(t, {
      args: ['--check-release'],
      existingBody: JSON.stringify(record),
      release: {
        tag: 'v1.3.9',
        refOid: 'c'.repeat(40),
        sha: 'd'.repeat(40),
      },
    })

    assert.equal(result.status, 0, result.stderr)
    assert.deepEqual(JSON.parse(result.stdout), { state: 'superseded' })
    assert.equal(result.output, 'state=superseded\n')
    assert.equal(result.requests.length, 1)
    assert.equal(result.requests[0].method, 'GET')
    assert.deepEqual(result.uploads, [])
  })
}

for (const outcome of ['publishing', 'succeeded']) {
  test(`reused-build ${outcome} state supports immutable release replay`, async (t) => {
    const record = { ...publishedRecord(outcome), ci_sha: 'c'.repeat(40) }
    const result = await runStateCommand(t, {
      existingBody: JSON.stringify(record),
      args: ['--check-release'],
      release: { tag: record.tag, refOid: record.ref_oid, sha: record.sha },
    })
    assert.equal(result.status, 0, result.stderr)
    assert.equal(
      JSON.parse(result.stdout).state,
      outcome === 'succeeded' ? 'already-released' : 'retry'
    )
  })
}
