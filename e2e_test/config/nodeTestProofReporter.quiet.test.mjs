// Recorder stays quiet unless RECORD_NODE_TEST_PROOF=1 in a usable Git
// checkout, and ordinary runs leave retained proof untouched.
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  git,
  maskTiming,
  readLatest,
  runNode,
  scratchCheckout,
  writeTest,
} from './nodeTestProofReporterFixture.mjs'

test('option off leaves no proof file', (t) => {
  const fixture = scratchCheckout(t)
  writeTest(
    fixture,
    'scripts/silent.test.mjs',
    `import { test } from 'node:test'
test('passes', () => {})
`
  )
  git(fixture, ['add', 'scripts/silent.test.mjs'])
  git(fixture, ['commit', '-m', 'add test'])

  const result = runNode(fixture, ['scripts/silent.test.mjs'], {
    record: false,
  })
  assert.equal(result.status, 0, result.stderr || result.stdout)
  assert.equal(readLatest(fixture), undefined)
  assert.equal(existsSync(path.join(fixture, 'scripts/proof')), false)
})

test('outside Git the recorder is a no-op', (t) => {
  const fixture = scratchCheckout(t, { inGit: false })
  writeTest(
    fixture,
    'scripts/nongit.test.mjs',
    `import { test } from 'node:test'
test('passes', () => {})
`
  )

  const result = runNode(fixture, ['scripts/nongit.test.mjs'], {
    record: true,
  })
  assert.equal(result.status, 0, result.stderr || result.stdout)
  assert.equal(readLatest(fixture), undefined)
  assert.equal(existsSync(path.join(fixture, 'scripts/proof')), false)
})

test('stdout stderr and exit are identical with and without the option', (t) => {
  const fixture = scratchCheckout(t)
  writeTest(
    fixture,
    'scripts/identical.test.mjs',
    `import { test } from 'node:test'
test('passes', () => {})
`
  )
  writeTest(
    fixture,
    'scripts/identical-fail.test.mjs',
    `import assert from 'node:assert/strict'
import { test } from 'node:test'
test('fails', () => {
  assert.equal('actual', 'expected')
})
`
  )
  git(fixture, ['add', '.'])
  git(fixture, ['commit', '-m', 'identical'])

  for (const file of [
    'scripts/identical.test.mjs',
    'scripts/identical-fail.test.mjs',
  ]) {
    const off = runNode(fixture, [file], { record: false })
    const on = runNode(fixture, [file], { record: true })
    assert.equal(on.status, off.status, file)
    assert.equal(maskTiming(on.stdout), maskTiming(off.stdout), file)
    assert.equal(maskTiming(on.stderr), maskTiming(off.stderr), file)
  }
})

test('a normal run after recording leaves the snapshot unchanged', (t) => {
  const fixture = scratchCheckout(t)
  writeTest(
    fixture,
    'scripts/unchanged.test.mjs',
    `import { test } from 'node:test'
test('recorded', () => {})
`
  )
  git(fixture, ['add', '.'])
  git(fixture, ['commit', '-m', 'unchanged'])

  assert.equal(
    runNode(fixture, ['scripts/unchanged.test.mjs'], { record: true }).status,
    0
  )
  const before = readFileSync(
    path.join(fixture, 'scripts/proof/latest.json'),
    'utf8'
  )
  assert.equal(
    runNode(fixture, ['scripts/unchanged.test.mjs'], { record: false }).status,
    0
  )
  assert.equal(
    readFileSync(path.join(fixture, 'scripts/proof/latest.json'), 'utf8'),
    before
  )
})
