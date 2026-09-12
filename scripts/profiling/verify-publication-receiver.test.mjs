import assert from 'node:assert/strict'
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { test } from 'node:test'
import { findPublicationReceiverMismatch } from './verify-publication-receiver.mjs'

function makeCheckout(t) {
  const root = mkdtempSync(join(tmpdir(), 'verify-publication-receiver-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  return root
}

function write(checkoutDir, relativePath, content) {
  const filePath = join(checkoutDir, relativePath)
  mkdirSync(dirname(filePath), { recursive: true })
  writeFileSync(filePath, content)
}

test('matches multiple folders and exact bytes', (t) => {
  const checkoutDir = makeCheckout(t)
  const files = [
    { relativePath: 'group-00/Added-00000.md', content: 'alpha\n' },
    { relativePath: 'group-01/Added-00001.md', content: 'beta\n' },
    { relativePath: 'group-02/nested/Added-00002.md', content: 'gamma\n' },
  ]
  for (const { relativePath, content } of files) {
    write(checkoutDir, relativePath, content)
  }

  assert.equal(findPublicationReceiverMismatch(checkoutDir, files), null)
})

test('reports a missing file by path', (t) => {
  const checkoutDir = makeCheckout(t)
  write(checkoutDir, 'group-00/present.md', 'present\n')
  const files = [
    { relativePath: 'group-00/present.md', content: 'present\n' },
    { relativePath: 'group-00/absent.md', content: 'absent\n' },
  ]

  assert.deepEqual(findPublicationReceiverMismatch(checkoutDir, files), {
    relativePath: 'group-00/absent.md',
    reason: 'missing',
  })
})

test('reports changed content by path', (t) => {
  const checkoutDir = makeCheckout(t)
  write(checkoutDir, 'group-00/changed.md', 'actual content\n')
  const files = [
    { relativePath: 'group-00/changed.md', content: 'expected content\n' },
  ]

  assert.deepEqual(findPublicationReceiverMismatch(checkoutDir, files), {
    relativePath: 'group-00/changed.md',
    reason: 'changed',
  })
})

test('reports a newline-only difference by path', (t) => {
  const checkoutDir = makeCheckout(t)
  write(checkoutDir, 'group-00/note.md', 'same content')
  const files = [
    { relativePath: 'group-00/note.md', content: 'same content\n' },
  ]

  assert.deepEqual(findPublicationReceiverMismatch(checkoutDir, files), {
    relativePath: 'group-00/note.md',
    reason: 'changed',
  })
})
