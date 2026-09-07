import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  runStateCommand,
  selectedReleaseRecord,
} from './application-release-state-fixtures.mjs'

test('the selected release identity is serialized before publication', async (t) => {
  const result = await runStateCommand(t, {
    args: ['--select-release'],
    release: {
      tag: selectedReleaseRecord.tag,
      refOid: selectedReleaseRecord.ref_oid,
      sha: selectedReleaseRecord.sha,
    },
  })

  assert.equal(result.status, 0, result.stderr)
  assert.deepEqual(JSON.parse(result.stdout), {
    state: 'selected',
    record: selectedReleaseRecord,
  })
  assert.deepEqual(result.uploads.map(JSON.parse), [selectedReleaseRecord])
  assert.equal(result.requests.length, 1)
  const upload = result.requests[0]
  assert.equal(upload.method, 'POST')
  assert.deepEqual(Object.fromEntries(upload.url.searchParams), {
    uploadType: 'media',
    name: 'deploy/application-release.json',
  })
})

test('a malformed selected identity fails without a write', async (t) => {
  const result = await runStateCommand(t, {
    args: ['--select-release'],
    release: {
      tag: selectedReleaseRecord.tag,
      refOid: selectedReleaseRecord.ref_oid,
      sha: 'not-an-object-id',
    },
  })

  assert.equal(result.status, 1)
  assert.match(result.stderr, /RELEASE_SHA is invalid/)
  assert.deepEqual(result.requests, [])
  assert.deepEqual(result.uploads, [])
})

test('a selected state transport failure fails without a write', async (t) => {
  const result = await runStateCommand(t, {
    args: ['--select-release'],
    release: {
      tag: selectedReleaseRecord.tag,
      refOid: selectedReleaseRecord.ref_oid,
      sha: selectedReleaseRecord.sha,
    },
    unavailable: true,
  })

  assert.equal(result.status, 1)
  assert.match(result.stderr, /GCS request failed/)
  assert.deepEqual(result.requests, [])
  assert.deepEqual(result.uploads, [])
})
