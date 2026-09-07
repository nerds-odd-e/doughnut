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
  runWrapper,
  runWrapperAsync,
  waitForOneOwnerAndOneRefusal,
} from './backend-test-worktree-launcher-fixtures.mjs'
import {
  lockPaths,
  writeStaleOwnerLock,
} from './backend-test-worktree-lock-fixtures.mjs'

function assertRefusedByActiveOwner(result) {
  assert.notEqual(result.status, 0)
  assert.match(outputOf(result), /already running/i)
  assert.doesNotMatch(outputOf(result), /GRADLE_STDOUT|GRADLE_REACHED/)
}

const configuredId = 'wt_a7c2'

const ordinaryMigrate = {
  command: 'backend/gradlew',
  args: ['-p', 'backend', 'migrateTestDB'],
}

function configuredCheckout(t) {
  return makeCheckout(t, {
    config: JSON.stringify({ id: configuredId }),
  })
}

async function assertHeldOwnerRefusesCompetitor(
  t,
  { startOwner, startCompetitor }
) {
  const checkout = configuredCheckout(t)
  const owner = startOwner(checkout)
  await owner.waitForGradleReached()
  assertRefusedByActiveOwner(startCompetitor(checkout))
  owner.release()
  assert.equal((await owner.waitForExit()).status, 0)
}

function assertReclaimedConfiguredOwner(checkout, result) {
  assert.equal(result.status, 0, outputOf(result))
  assert.equal(
    readGradleInvocation(checkout).url,
    jdbcUrl(`doughnut_${configuredId}_test`)
  )
  assert.equal(
    readFileSync(lockPaths(checkout).ownerFile, 'utf8').trim(),
    String(result.pid)
  )
}

test('active owner refuses a second launcher before it reads a malformed replacement config or reaches gradle', async (t) => {
  const checkout = configuredCheckout(t)
  const owner = runLauncherAsync(checkout, { env: { FAKE_GRADLE_EXIT: '3' } })
  await owner.waitForGradleReached()

  writeFileSync(path.join(checkout.root, '.worktree.local.json'), '{"id":')

  const second = runLauncher(checkout)
  assertRefusedByActiveOwner(second)
  assert.doesNotMatch(outputOf(second), /SyntaxError/i)

  owner.release()
  const ownerResult = await owner.waitForExit()
  assert.equal(ownerResult.status, 3)
})

test('a malformed owner lock record refuses rather than being reclaimed', (t) => {
  const checkout = configuredCheckout(t)
  const { dir, ownerFile } = lockPaths(checkout)
  mkdirSync(dir)
  writeFileSync(ownerFile, 'not-a-pid')

  const result = runLauncher(checkout)
  assertRefusedBeforeGradle(checkout, result)
  assert.match(outputOf(result), /already running/i)
})

test('a different checkout root reaches its own gradle stand-in while another checkout is locked', async (t) => {
  const heldCheckout = configuredCheckout(t)
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

test('active ordinary migrate refuses an overlapping opt-in launcher before gradle', (t) =>
  assertHeldOwnerRefusesCompetitor(t, {
    startOwner: (checkout) => runWrapperAsync(checkout, ordinaryMigrate),
    startCompetitor: runLauncher,
  }))

test('active ordinary migrate refuses an overlapping ordinary migrate before gradle', (t) =>
  assertHeldOwnerRefusesCompetitor(t, {
    startOwner: (checkout) => runWrapperAsync(checkout, ordinaryMigrate),
    startCompetitor: (checkout) => runWrapper(checkout, ordinaryMigrate),
  }))

test('active opt-in launcher refuses an overlapping ordinary migrate before gradle', (t) =>
  assertHeldOwnerRefusesCompetitor(t, {
    startOwner: runLauncherAsync,
    startCompetitor: (checkout) => runWrapper(checkout, ordinaryMigrate),
  }))

test('a stale owner record for an exited process is reclaimed and the launcher reaches gradle against the configured database', (t) => {
  const checkout = configuredCheckout(t)
  writeStaleOwnerLock(checkout)
  assertReclaimedConfiguredOwner(checkout, runLauncher(checkout))
})

test('a stale owner record is reclaimed and the ordinary wrapper reaches gradle against the configured database', (t) => {
  const checkout = configuredCheckout(t)
  writeStaleOwnerLock(checkout)
  assertReclaimedConfiguredOwner(
    checkout,
    runWrapper(checkout, ordinaryMigrate)
  )
})

test('two overlapping reclaimers of a stale lock leave only one gradle owner', {
  timeout: 10000,
}, async (t) => {
  const checkout = configuredCheckout(t)
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
  assertRefusedByActiveOwner(refused)
  assert.equal(
    readGradleInvocation(checkout).url,
    jdbcUrl(`doughnut_${configuredId}_test`)
  )

  owner.release()
  const ownerResult = await ownerExit
  assert.equal(ownerResult.status, 0, outputOf(ownerResult))
})
