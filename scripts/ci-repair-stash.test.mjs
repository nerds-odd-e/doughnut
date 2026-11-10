import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { test } from 'node:test'

const cli = resolve(
  '.agents/skills/dough-execute-plan/scripts/ci-repair-stash.mjs'
)
const content = (first, last) =>
  [first, ...Array.from({ length: 12 }, (_, i) => `line ${i}`), last, ''].join(
    '\n'
  )

function fixture(t, staged = false) {
  const root = mkdtempSync(join(tmpdir(), 'stash-recovery-test-'))
  t.after(() => rmSync(root, { recursive: true, force: true }))
  const git = (...args) => {
    const result = spawnSync('git', args, { cwd: root, encoding: 'utf8' })
    assert.equal(result.status, 0, result.stderr)
    return result.stdout.trim()
  }
  const write = (name, value) => writeFileSync(join(root, name), value)
  const run = (command, ...args) => {
    const result = spawnSync(process.execPath, [cli, command, ...args], {
      cwd: root,
      encoding: 'utf8',
    })
    assert.ok(result.stdout, result.stderr)
    return JSON.parse(result.stdout)
  }
  git('init', '-b', 'story')
  git('config', 'user.name', 'Test')
  git('config', 'user.email', 'test@example.com')
  write('merged.txt', content('base', 'base'))
  write('conflicted.txt', 'base\n')
  git('add', '.')
  git('commit', '-qm', 'base')
  write('foreign.txt', 'other writer\n')
  git('stash', 'push', '-u', '-m', 'other writer')
  const foreign = git('rev-parse', 'stash@{0}')
  write('merged.txt', content('draft', 'base'))
  write('conflicted.txt', 'draft\n')
  if (staged) git('add', 'conflicted.txt')
  const saved = run('save', '--checkout', root, '--label', 'owned repair')
  t.after(() =>
    rmSync(resolve(saved.record, '..'), { recursive: true, force: true })
  )
  write('merged.txt', content('base', 'repair'))
  write('conflicted.txt', 'repair\n')
  git('add', '.')
  git('commit', '-qm', 'repair')
  return { root, git, write, run, saved, foreign }
}

test('restores merged draft plus repair, resolves a legacy record, and drops only its stash', (t) => {
  const { root, git, write, run, saved, foreign } = fixture(t)
  const restored = run('restore', '--record', saved.record)
  assert.equal(restored.status, 'conflict')
  assert.equal(restored.applied, 'partial')
  assert.equal(
    readFileSync(join(root, 'merged.txt'), 'utf8'),
    content('draft', 'repair')
  )
  assert.equal(
    run('resolve', '--record', saved.record, '--reviewed-oid', saved.oid)
      .status,
    'unresolved'
  )
  write('conflicted.txt', 'draft\nrepair\n')
  git('add', 'conflicted.txt')
  git('restore', '--staged', '.')
  // Existing records from the released classifier retain this false-negative.
  const record = JSON.parse(readFileSync(saved.record, 'utf8'))
  record.restore.applied = 'none'
  writeFileSync(saved.record, JSON.stringify(record))
  assert.equal(
    run('resolve', '--record', saved.record, '--reviewed-oid', saved.oid)
      .status,
    'resolved'
  )
  assert.equal(run('drop', '--record', saved.record).status, 'dropped')
  assert.equal(git('stash', 'list', '--format=%H'), foreign)
  assert.equal(
    readFileSync(join(root, 'conflicted.txt'), 'utf8'),
    'draft\nrepair\n'
  )
})

test('refuses ordinary drop when restore did not apply, and requires exact reviewed identity', (t) => {
  const { run, saved, write, git } = fixture(t)
  write('conflicted.txt', 'unfinished repair\n')
  const restored = run('restore', '--record', saved.record)
  assert.equal(restored.applied, 'none')
  assert.equal(run('drop', '--record', saved.record).status, 'unapplied')
  assert.equal(
    run('resolve', '--record', saved.record, '--reviewed-oid', 'wrong').status,
    'review-required'
  )
  git('switch', '-c', 'another-story')
  assert.equal(
    run('resolve', '--record', saved.record, '--reviewed-oid', saved.oid)
      .status,
    'checkout-mismatch'
  )
  assert.ok(git('stash', 'list', '--format=%H').includes(saved.oid))
})

test('reviewed resolution retains the stash until original staging is restored', (t) => {
  const { run, saved, write } = fixture(t, true)
  assert.equal(run('restore', '--record', saved.record).status, 'conflict')
  write('conflicted.txt', 'draft\nrepair\n')
  const refused = run(
    'resolve',
    '--record',
    saved.record,
    '--reviewed-oid',
    saved.oid
  )
  assert.equal(refused.status, 'partial')
  assert.deepEqual(refused.stagedNotRestored, ['conflicted.txt'])
})
