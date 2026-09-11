import assert from 'node:assert/strict'
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  jdbcUrl,
  makeCheckout,
  readGradleInvocation,
  readGradlePid,
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
  assert.match(
    outputOf(result),
    /already running|retirement admission gate is already held/i
  )
  assert.doesNotMatch(outputOf(result), /GRADLE_STDOUT|GRADLE_REACHED/)
}

const configuredId = 'wt_a7c2'

const ordinaryMigrate = {
  command: 'backend/gradlew',
  args: ['-p', 'backend', 'migrateTestDB'],
}

const ordinaryTest = {
  command: 'backend/gradlew',
  args: ['-p', 'backend', 'test'],
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

async function assertSupervisedInterruption(t, startOwner) {
  const checkout = configuredCheckout(t)
  const owner = startOwner(checkout)
  await owner.waitForGradleReached()

  assert.equal(
    readFileSync(lockPaths(checkout).ownerFile, 'utf8').trim(),
    String(owner.pid)
  )
  assert.notEqual(readGradlePid(checkout), owner.pid)
  assertRefusedByActiveOwner(runLauncher(checkout))

  owner.signalProcessGroup('SIGINT')
  const result = await owner.waitForExit()
  assert.equal(result.status, null, outputOf(result))
  assert.equal(result.signal, 'SIGINT', outputOf(result))
  assert.equal(existsSync(lockPaths(checkout).dir), false)
}

async function assertVerifiedCancellation(
  t,
  { startOwner, waitUntilActive, captureWorker, releaseShutdown, signal }
) {
  const checkout = configuredCheckout(t)
  const owner = startOwner(checkout)
  await waitUntilActive(owner)
  const workerPid = captureWorker(checkout)

  owner.signalProcessGroup(signal)
  await owner.waitForSignalReceived()
  assertRefusedByActiveOwner(runLauncher(checkout))
  assert.equal(existsSync(lockPaths(checkout).dir), true)

  releaseShutdown(owner)
  const result = await owner.waitForExit()
  assert.equal(result.status, null, outputOf(result))
  assert.equal(result.signal, signal, outputOf(result))
  assert.equal(existsSync(lockPaths(checkout).dir), false)
  assert.throws(() => process.kill(workerPid, 0), { code: 'ESRCH' })
}

function assertReclaimedConfiguredOwner(checkout, result) {
  assert.equal(result.status, 0, outputOf(result))
  assert.equal(
    readGradleInvocation(checkout).url,
    jdbcUrl(`doughnut_${configuredId}_test`)
  )
  assert.equal(existsSync(lockPaths(checkout).dir), false)
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

test('opt-in launcher observes its Gradle child through interruption', (t) =>
  assertSupervisedInterruption(t, (checkout) =>
    runLauncherAsync(checkout, { detached: true })
  ))

test('ordinary migrate observes its Gradle child through interruption', (t) =>
  assertSupervisedInterruption(t, (checkout) =>
    runWrapperAsync(checkout, { ...ordinaryMigrate, detached: true })
  ))

test('handled cancellation during preparation holds ownership until mysql stops, then releases it', (t) =>
  assertVerifiedCancellation(t, {
    startOwner: (checkout) =>
      runLauncherAsync(checkout, {
        detached: true,
        env: { MYSQL_HOLD: '1', FAKE_DELAY_SIGNAL_EXIT: '1' },
      }),
    waitUntilActive: (owner) => owner.waitForMysqlReached(),
    captureWorker: (checkout) =>
      Number(readFileSync(path.join(checkout.root, 'mysql-pid.1'), 'utf8')),
    releaseShutdown: (owner) => owner.releaseMysql(),
    signal: 'SIGINT',
  }))

test('handled cancellation during preliminary migration holds ownership until gradle stops, then releases it', (t) =>
  assertVerifiedCancellation(t, {
    startOwner: (checkout) =>
      runWrapperAsync(checkout, {
        ...ordinaryTest,
        detached: true,
        env: { FAKE_DELAY_SIGNAL_EXIT: '1' },
      }),
    waitUntilActive: (owner) => owner.waitForGradleReached(),
    captureWorker: (checkout) => readGradlePid(checkout),
    releaseShutdown: (owner) => owner.release(),
    signal: 'SIGTERM',
  }))

test('handled cancellation during final workload holds ownership until gradle stops, then releases it', (t) =>
  assertVerifiedCancellation(t, {
    startOwner: (checkout) =>
      runWrapperAsync(checkout, {
        ...ordinaryTest,
        detached: true,
        env: {
          FAKE_DELAY_SIGNAL_EXIT: '1',
          GRADLE_HOLD_INVOCATION: '2',
        },
      }),
    waitUntilActive: (owner) => owner.waitForGradleReached(),
    captureWorker: (checkout) => readGradlePid(checkout, 2),
    releaseShutdown: (owner) => owner.release(),
    signal: 'SIGINT',
  }))

test('cancelling one checkout leaves an unrelated checkout owner alive', async (t) => {
  const checkout = configuredCheckout(t)
  const peerCheckout = makeCheckout(t, {
    config: JSON.stringify({ id: 'wt_peer' }),
  })
  const owner = runLauncherAsync(checkout, {
    detached: true,
    env: { FAKE_DELAY_SIGNAL_EXIT: '1' },
  })
  const peer = runLauncherAsync(peerCheckout, { detached: true })
  t.after(() => {
    owner.stop()
    peer.stop()
  })
  await Promise.all([owner.waitForGradleReached(), peer.waitForGradleReached()])

  owner.signalProcessGroup('SIGINT')
  await owner.waitForSignalReceived()
  assert.doesNotThrow(() => process.kill(peer.pid, 0))
  owner.release()
  assert.equal((await owner.waitForExit()).signal, 'SIGINT')
  assert.doesNotThrow(() => process.kill(peer.pid, 0))

  peer.release()
  assert.equal((await peer.waitForExit()).status, 0)
})

test('cancellation remains the outcome when releasing changed ownership fails visibly', async (t) => {
  const checkout = configuredCheckout(t)
  const owner = runLauncherAsync(checkout, {
    detached: true,
    env: { FAKE_DELAY_SIGNAL_EXIT: '1' },
  })
  await owner.waitForGradleReached()
  writeFileSync(lockPaths(checkout).ownerFile, '424242')

  owner.signalProcessGroup('SIGINT')
  await owner.waitForSignalReceived()
  owner.release()
  const result = await owner.waitForExit()
  assert.equal(result.signal, 'SIGINT', outputOf(result))
  assert.match(outputOf(result), /Failed to release/)
  assert.equal(readFileSync(lockPaths(checkout).ownerFile, 'utf8'), '424242')
})

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
  assert.equal(existsSync(lockPaths(checkout).dir), false)
})

test('changed ownership is preserved and failed release is visible', async (t) => {
  const checkout = configuredCheckout(t)
  const owner = runLauncherAsync(checkout)
  await owner.waitForGradleReached()
  writeFileSync(lockPaths(checkout).ownerFile, '424242')

  owner.release()
  const result = await owner.waitForExit()
  assert.notEqual(result.status, 0, outputOf(result))
  assert.match(outputOf(result), /ownership changed before release/)
  assert.equal(readFileSync(lockPaths(checkout).ownerFile, 'utf8'), '424242')
})

test('failed release retains an existing workload failure status', async (t) => {
  const checkout = configuredCheckout(t)
  const owner = runLauncherAsync(checkout, { env: { FAKE_GRADLE_EXIT: '7' } })
  await owner.waitForGradleReached()
  writeFileSync(lockPaths(checkout).ownerFile, '424242')

  owner.release()
  const result = await owner.waitForExit()
  assert.equal(result.status, 7, outputOf(result))
  assert.match(outputOf(result), /Failed to release/)
  assert.equal(readFileSync(lockPaths(checkout).ownerFile, 'utf8'), '424242')
})
