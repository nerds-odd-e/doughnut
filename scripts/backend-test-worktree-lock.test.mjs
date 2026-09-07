import assert from 'node:assert/strict'
import { mkdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  assertRefusedBeforeGradle,
  jdbcUrl,
  lockPaths,
  makeCheckout,
  outputOf,
  readGradleInvocation,
  runLauncher,
  runLauncherAsync,
} from './backend-test-worktree-test-fixtures.mjs'

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
