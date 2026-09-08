import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  allocateFreePort,
  closeServer,
  isPidAlive,
  isTcpListening,
  listenTcp,
  spawnForeignProcess,
  startLiveOwner,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import { processGroupId } from './sut-listener-pids.mjs'
import {
  spawnDetachedOwnedSupervisor,
  waitForDetachedBackendListener,
  waitForPeerPids,
  waitUntil,
  writeOwnedSupervisorRunPPeers,
} from './sut-owned-supervisor-fixtures.mjs'
import {
  acquireSutRunnerLease,
  claimSutOwnership,
  sutOwnerLockDir,
  verifyLiveSutOwner,
} from './sut-owner.mjs'
import { runSutRestart } from './sut-restart.mjs'
import { healthyOnce, makeStartSpy } from './sut-start-fixtures.mjs'

function lsofMustNotRun() {
  return (_cmd, _args, cb) => {
    cb(new Error('lsof must not run for isolated restart'))
  }
}

function assertPortBindable(port) {
  execFileSync(
    process.execPath,
    [
      '-e',
      `import net from 'node:net'
const server = net.createServer()
server.once('error', (error) => {
  console.error(error)
  process.exit(1)
})
server.listen(${port}, '127.0.0.1', () => {
  server.close(() => process.exit(0))
})
`,
    ],
    { stdio: 'pipe' }
  )
}

function assertTcpResponding(port) {
  execFileSync(
    process.execPath,
    [
      '-e',
      `import net from 'node:net'
const socket = net.createConnection({ host: '127.0.0.1', port: ${port} }, () => {
  socket.end()
  process.exit(0)
})
socket.on('error', () => process.exit(1))
setTimeout(() => process.exit(1), 1000)
`,
    ],
    { stdio: 'pipe' }
  )
}

test('idle live owner restart stops owned children, holds the claim, and starts on the same allocation', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  writeOwnedSupervisorRunPPeers(checkout.root)
  const owner = await claimSutOwnership(checkout.root)
  const foreign = spawnForeignProcess()
  const foreignListener = await listenTcp()
  const backendPort = await allocateFreePort()
  const { supervisor, state } = spawnDetachedOwnedSupervisor(t, {
    checkoutRoot: checkout.root,
    owner,
    logFile: path.join(checkout.root, 'sut.log'),
    env: { SUT_FIXTURE_BACKEND_PORT: String(backendPort) },
    afterCleanup: () => {
      try {
        foreign.kill('SIGKILL')
      } catch {
        // already gone
      }
      closeServer(foreignListener.server)
    },
  })

  await waitUntil(
    async () => (await verifyLiveSutOwner(checkout.root)).ok,
    5_000,
    'timed out waiting for live SUT owner'
  )
  state.pids = await waitForPeerPids(checkout.root)
  state.pids.backendListener = await waitForDetachedBackendListener(
    checkout.root,
    backendPort
  )
  const backendParentPgid = await processGroupId(state.pids.backend)
  const backendListenerPgid = await processGroupId(state.pids.backendListener)
  assert.notEqual(
    backendListenerPgid,
    backendParentPgid,
    'detached backend listener must be in a separate process group'
  )
  assert.equal(await isTcpListening(backendPort), true)

  const supervisorPid = supervisor.pid
  const ownedParentPid = state.pids.backend
  const ownedBackendPid = state.pids.backendListener
  const start = makeStartSpy()
  const code = await runSutRestart({
    checkoutRoot: checkout.root,
    execFileFn: lsofMustNotRun(),
    spawnFn: (...args) => {
      assert.equal(isPidAlive(ownedParentPid), false)
      assert.equal(isPidAlive(ownedBackendPid), false)
      assertPortBindable(backendPort)
      assertTcpResponding(foreignListener.port)
      return start.spawnFn(...args)
    },
    log: () => undefined,
    errLog: () => undefined,
    healthcheckFn: healthyOnce,
    databaseExistsFn: () => true,
    isPortOccupiedFn: async () => false,
    logFile: path.join(checkout.root, 'restart.log'),
    pidFile: path.join(checkout.root, 'restart.pid'),
    ownerStopTimeoutMs: 8_000,
  })

  assert.equal(code, 0)
  await waitUntil(
    () =>
      !(
        isPidAlive(state.pids.backend) ||
        isPidAlive(state.pids.backendListener) ||
        isPidAlive(state.pids.lb) ||
        isPidAlive(state.pids.frontend) ||
        isPidAlive(supervisorPid)
      ),
    5_000,
    'timed out waiting for owned peers and supervisor to exit'
  )
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), true)
  assert.equal(start.calls.length, 1)
  assert.equal(start.calls[0][2].env.SUT_OWNER_TOKEN, owner.token)
  assert.equal(start.calls[0][2].env.SUT_OWNER_CONTROL_PATH, owner.controlPath)
  assert.equal(isPidAlive(foreign.pid), true)
  assert.equal(await isTcpListening(foreignListener.port), true)
})

test('busy Cypress lease refuses isolated restart before signals', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())
  await acquireSutRunnerLease(checkout.root)
  const start = makeStartSpy()
  await assert.rejects(
    runSutRestart({
      checkoutRoot: checkout.root,
      execFileFn: lsofMustNotRun(),
      spawnFn: start.spawnFn,
      log: () => undefined,
    }),
    /Cypress runner|Refusing restart/i
  )
  assert.equal(start.calls.length, 0)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, true)
})

test('stale unverifiable owner refuses isolated restart before signals', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  await claimSutOwnership(checkout.root)
  const start = makeStartSpy()
  await assert.rejects(
    runSutRestart({
      checkoutRoot: checkout.root,
      execFileFn: lsofMustNotRun(),
      spawnFn: start.spawnFn,
      log: () => undefined,
    }),
    /verified live SUT owner|stale or unverifiable/i
  )
  assert.equal(start.calls.length, 0)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), true)
})
