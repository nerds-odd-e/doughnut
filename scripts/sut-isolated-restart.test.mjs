import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { existsSync, readFileSync } from 'node:fs'
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

async function readyOwnedDetachedBackendRestart(
  t,
  { ignoreTerm = false } = {}
) {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  writeOwnedSupervisorRunPPeers(checkout.root)
  const owner = await claimSutOwnership(checkout.root)
  const foreign = spawnForeignProcess()
  const foreignListener = await listenTcp()
  const backendPort = await allocateFreePort()
  const env = { SUT_FIXTURE_BACKEND_PORT: String(backendPort) }
  if (ignoreTerm) env.SUT_FIXTURE_BACKEND_IGNORE_TERM = '1'
  const { supervisor, state } = spawnDetachedOwnedSupervisor(t, {
    checkoutRoot: checkout.root,
    owner,
    logFile: path.join(checkout.root, 'sut.log'),
    env,
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
  assert.notEqual(
    await processGroupId(state.pids.backendListener),
    await processGroupId(state.pids.backend),
    'detached backend listener must be in a separate process group'
  )
  assert.equal(await isTcpListening(backendPort), true)

  return {
    checkout,
    owner,
    foreign,
    foreignListener,
    backendPort,
    supervisorPid: supervisor.pid,
    state,
    ownedParentPid: state.pids.backend,
    ownedBackendPid: state.pids.backendListener,
    termAckPath: path.join(checkout.root, 'backend-listener.term'),
  }
}

function assertOwnedBackendClearedBeforeStart(ctx) {
  assert.equal(isPidAlive(ctx.ownedParentPid), false)
  assert.equal(isPidAlive(ctx.ownedBackendPid), false)
  assertPortBindable(ctx.backendPort)
  assertTcpResponding(ctx.foreignListener.port)
}

async function restartAfterOwnedStop(ctx, { ownerStopTimeoutMs, beforeStart }) {
  const start = makeStartSpy()
  const code = await runSutRestart({
    checkoutRoot: ctx.checkout.root,
    execFileFn: lsofMustNotRun(),
    spawnFn: (...args) => {
      beforeStart()
      return start.spawnFn(...args)
    },
    log: () => undefined,
    errLog: () => undefined,
    healthcheckFn: healthyOnce,
    databaseExistsFn: () => true,
    isPortOccupiedFn: async () => false,
    logFile: path.join(ctx.checkout.root, 'restart.log'),
    pidFile: path.join(ctx.checkout.root, 'restart.pid'),
    ownerStopTimeoutMs,
  })
  assert.equal(code, 0)
  await waitUntil(
    () =>
      !(
        isPidAlive(ctx.state.pids.backend) ||
        isPidAlive(ctx.state.pids.backendListener) ||
        isPidAlive(ctx.state.pids.lb) ||
        isPidAlive(ctx.state.pids.frontend) ||
        isPidAlive(ctx.supervisorPid)
      ),
    5_000,
    'timed out waiting for owned peers and supervisor to exit'
  )
  assert.equal(existsSync(sutOwnerLockDir(ctx.checkout.root)), true)
  assert.equal(start.calls.length, 1)
  assert.equal(isPidAlive(ctx.foreign.pid), true)
  assert.equal(await isTcpListening(ctx.foreignListener.port), true)
  return start
}

test('idle live owner restart stops owned children, holds the claim, and starts on the same allocation', async (t) => {
  const ctx = await readyOwnedDetachedBackendRestart(t)
  const start = await restartAfterOwnedStop(ctx, {
    ownerStopTimeoutMs: 8_000,
    beforeStart: () => assertOwnedBackendClearedBeforeStart(ctx),
  })
  assert.equal(start.calls[0][2].env.SUT_OWNER_TOKEN, ctx.owner.token)
  assert.equal(
    start.calls[0][2].env.SUT_OWNER_CONTROL_PATH,
    ctx.owner.controlPath
  )
})

test('restart escalates past a TERM-ignoring owned backend before start', async (t) => {
  const ctx = await readyOwnedDetachedBackendRestart(t, { ignoreTerm: true })
  await restartAfterOwnedStop(ctx, {
    ownerStopTimeoutMs: 15_000,
    beforeStart: () => {
      assertOwnedBackendClearedBeforeStart(ctx)
      assert.equal(readFileSync(ctx.termAckPath, 'utf8'), 'term')
    },
  })
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
