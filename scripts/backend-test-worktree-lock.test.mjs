import assert from 'node:assert/strict'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  jdbcUrl,
  makeCheckout,
  readGradleInvocation,
} from './backend-test-worktree-stand-in-fixtures.mjs'
import {
  assertRefusedBeforeGradle,
  outputOf,
  runLauncher,
  runLauncherAsync,
  waitForOneOwnerAndOneRefusal,
} from './backend-test-worktree-launcher-fixtures.mjs'
import {
  lockPaths,
  writeStaleOwnerLock,
} from './backend-test-worktree-lock-fixtures.mjs'

test('active owner refuses a second launcher before it reads a malformed replacement config or reaches gradle', async (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const owner = runLauncherAsync(checkout, { env: { FAKE_GRADLE_EXIT: '3' } })
  await owner.waitForGradleReached()

  writeFileSync(path.join(checkout.root, '.worktree.local.json'), '{"id":')

  const second = runLauncher(checkout)
  assert.notEqual(second.status, 0)
  assert.match(outputOf(second), /already running/i)
  assert.doesNotMatch(outputOf(second), /SyntaxError/i)
  assert.doesNotMatch(outputOf(second), /GRADLE_STDOUT|GRADLE_REACHED/)

  owner.release()
  const ownerResult = await owner.waitForExit()
  assert.equal(ownerResult.status, 3)
})

test('a malformed owner lock record refuses rather than being reclaimed', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const { dir, ownerFile } = lockPaths(checkout)
  mkdirSync(dir)
  writeFileSync(ownerFile, 'not-a-pid')

  const result = runLauncher(checkout)
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /already running/i)
})

test('a different checkout root reaches its own gradle stand-in while another checkout is locked', async (t) => {
  const heldCheckout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  const owner = runLauncherAsync(heldCheckout)
  await owner.waitForGradleReached()

  const otherCheckout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_b3d1' }),
  })
  const otherResult = runLauncher(otherCheckout)
  assert.equal(otherResult.status, 0, otherResult.stderr)
  assert.equal(
    readGradleInvocation(otherCheckout).url,
    jdbcUrl('doughnut_wt_b3d1_test')
  )

  owner.release()
  const ownerResult = await owner.waitForExit()
  assert.equal(ownerResult.status, 0)
})

test('a stale owner record for an exited process is reclaimed and the launcher reaches gradle against the configured database', (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  writeStaleOwnerLock(checkout)

  const result = runLauncher(checkout)
  assert.equal(result.status, 0, outputOf(result))
  assert.equal(
    readGradleInvocation(checkout).url,
    jdbcUrl('doughnut_wt_a7c2_test')
  )
  assert.equal(
    readFileSync(lockPaths(checkout).ownerFile, 'utf8').trim(),
    String(result.pid)
  )
})

test('two overlapping reclaimers of a stale lock leave only one gradle owner', {
  timeout: 10000,
}, async (t) => {
  const checkout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_a7c2' }),
  })
  writeStaleOwnerLock(checkout)

  const first = runLauncherAsync(checkout)
  const second = runLauncherAsync(checkout)
  t.after(() => {
    first.stop()
    second.stop()
  })

  const { refused, owner, ownerExit } = await waitForOneOwnerAndOneRefusal(
    first,
    second
  )
  assert.notEqual(refused.status, 0)
  assert.match(outputOf(refused), /already running/i)
  assert.doesNotMatch(outputOf(refused), /GRADLE_STDOUT|GRADLE_REACHED/)
  assert.equal(
    readGradleInvocation(checkout).url,
    jdbcUrl('doughnut_wt_a7c2_test')
  )

  owner.release()
  const ownerResult = await ownerExit
  assert.equal(ownerResult.status, 0, outputOf(ownerResult))
})
