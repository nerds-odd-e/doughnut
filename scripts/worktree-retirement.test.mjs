import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { mkdirSync, realpathSync, rmSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  makeLinkedWorktreeCheckout,
  makePrimaryCheckout,
} from './backend-test-worktree-linked-fixtures.mjs'
import { writeIsolatedConfig } from './sut-isolated-fixtures.mjs'
import {
  inspectDisposableDatabaseTargets,
  runWorktreeRetire,
} from './worktree-retirement.mjs'
import {
  clearEvidenceDeps,
  makeWritable,
  runCheck,
} from './worktree-retirement-test-helpers.mjs'

function git(cwd, args) {
  const result = spawnSync('git', args, {
    cwd,
    encoding: 'utf8',
    env: {
      ...process.env,
      GIT_AUTHOR_NAME: 'Test',
      GIT_AUTHOR_EMAIL: 'test@example.com',
      GIT_COMMITTER_NAME: 'Test',
      GIT_COMMITTER_EMAIL: 'test@example.com',
    },
  })
  if (result.status !== 0) {
    throw new Error(
      `git ${args.join(' ')} failed: ${result.stderr || result.stdout}`
    )
  }
  return result
}

function primaryRootFromLinked(linkedRoot) {
  const result = spawnSync(
    'git',
    ['-C', linkedRoot, 'rev-parse', '--git-common-dir'],
    { encoding: 'utf8' }
  )
  if (result.status !== 0) {
    throw new Error(result.stderr || result.stdout)
  }
  return path.dirname(path.resolve(linkedRoot, result.stdout.trim()))
}

function addPeerLinkedWorktree(t, linkedRoot) {
  const primaryRoot = primaryRootFromLinked(linkedRoot)
  const peerRoot = `${primaryRoot}-peer`
  git(primaryRoot, ['worktree', 'add', '--detach', peerRoot])
  t.after(() => {
    spawnSync(
      'git',
      ['-C', primaryRoot, 'worktree', 'remove', '--force', peerRoot],
      {
        encoding: 'utf8',
      }
    )
    rmSync(peerRoot, { recursive: true, force: true })
  })
  return peerRoot
}

test('linked checkout with identity reports exact unit target', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 0, result.err)
  assert.match(result.out, /inspection only/i)
  assert.match(result.out, /Worktree id: wt_a7c2/)
  assert.match(result.out, /Unit database: doughnut_wt_a7c2_test/)
  assert.match(result.out, /Idleness verification incomplete/)
  assert.equal(result.out.includes('E2E database:'), false)
  assert.deepEqual(inspectDisposableDatabaseTargets(checkout.root), {
    id: 'wt_a7c2',
    unitDatabase: 'doughnut_wt_a7c2_test',
    e2eDatabase: undefined,
  })
})

test('linked checkout with canonical E2E allocation reports both targets', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, {
    id: 'wt_a7c2',
    e2e: { database: 'doughnut_e2e_wt_a7c2' },
  })

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 0, result.err)
  assert.match(result.out, /Unit database: doughnut_wt_a7c2_test/)
  assert.match(result.out, /E2E database: doughnut_e2e_wt_a7c2/)
  assert.match(result.out, /Idleness verification incomplete/)
})

test('mutation mode without --check refuses visibly', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })
  const out = makeWritable()
  const err = makeWritable()

  const code = await runWorktreeRetire({
    argv: [],
    checkoutRoot: checkout.root,
    out,
    err,
    evidenceDeps: clearEvidenceDeps,
  })

  assert.equal(code, 1)
  assert.equal(out.content(), '')
  assert.match(err.content(), /mutation mode is not available yet/)
  assert.match(err.content(), /Usage: pnpm worktree:retire --check/)
})

test('primary checkout refuses inspection', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 1)
  assert.equal(result.out, '')
  assert.match(result.err, /not a Git linked worktree/)
})

test('linked checkout without identity refuses inspection', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 1)
  assert.match(result.err, /missing identity file/)
})

test('invalid identity refuses inspection', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'not_a_worktree_id' })

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 1)
  assert.match(result.err, /invalid identity/)
})

test('custom E2E database target refuses inspection', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, {
    id: 'wt_a7c2',
    e2e: { database: 'doughnut_e2e_test' },
  })

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 1)
  assert.match(result.err, /not the canonical doughnut_e2e_wt_a7c2/)
  assert.equal(result.err.includes('DROP'), false)
})

test('duplicate identity in another registered checkout refuses', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  writeIsolatedConfig(checkout.root, { id: 'wt_a7c2' })
  const peerRoot = addPeerLinkedWorktree(t, checkout.root)
  mkdirSync(peerRoot, { recursive: true })
  writeFileSync(
    path.join(peerRoot, '.worktree.local.json'),
    JSON.stringify({ id: 'wt_a7c2' })
  )

  const result = await runCheck(checkout.root)
  assert.equal(result.code, 1)
  assert.match(result.err, /also recorded in another registered checkout/)
  assert.ok(result.err.includes(realpathSync(peerRoot)))
})
