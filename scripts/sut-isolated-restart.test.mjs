import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { existsSync, writeFileSync } from 'node:fs'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  closeServer,
  isPidAlive,
  isTcpListening,
  listenTcp,
  spawnForeignProcess,
  startLiveOwner,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  acquireSutRunnerLease,
  claimSutOwnership,
  sutOwnerLockDir,
  verifyLiveSutOwner,
} from './sut-owner.mjs'
import { runSutRestart } from './sut-restart.mjs'
import { healthyOnce, makeStartSpy } from './sut-start-fixtures.mjs'

const worktreeRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)
const runPBin = path.join(worktreeRoot, 'node_modules/.bin/run-p')
const peerNames = ['backend', 'lb', 'frontend']

function pause(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function waitUntil(predicate, timeoutMs, message) {
  const deadline = Date.now() + timeoutMs
  let last
  while (Date.now() < deadline) {
    last = await predicate()
    if (last) return last
    await pause(50)
  }
  throw new Error(message)
}

function lsofMustNotRun() {
  return (_cmd, _args, cb) => {
    cb(new Error('lsof must not run for isolated restart'))
  }
}

function writeRunPPeers(checkoutRoot) {
  writeFileSync(
    path.join(checkoutRoot, 'package.json'),
    `${JSON.stringify({
      name: 'sut-isolated-restart-fixture',
      private: true,
      scripts: {
        'backend:sut': 'node ./peer.mjs backend',
        'local:lb:vite': 'node ./peer.mjs lb',
        'frontend:sut': 'node ./peer.mjs frontend',
      },
    })}\n`
  )
  writeFileSync(
    path.join(checkoutRoot, 'peer.mjs'),
    `import { writeFileSync } from 'node:fs'
writeFileSync(new URL(\`./\${process.argv[2]}.pid\`, import.meta.url), String(process.pid))
setInterval(() => {}, 1000)
`
  )
  writeFileSync(
    path.join(checkoutRoot, 'run-supervisor.mjs'),
    `import { spawn } from 'node:child_process'
import { startOwnedSutSupervisor, sutServiceArgs } from ${JSON.stringify(
      path.join(worktreeRoot, 'scripts/sut-services.mjs')
    )}

await startOwnedSutSupervisor({
  checkoutRoot: process.env.SUT_CHECKOUT_ROOT,
  logFile: process.env.SUT_LOG_FILE,
  env: process.env,
  serviceArgs: sutServiceArgs({}),
  spawnFn: (_cmd, args, opts) => {
    const runP = args.indexOf('run-p')
    return spawn(${JSON.stringify(runPBin)}, args.slice(runP + 1), opts)
  },
})
`
  )
}

async function readPeerPids(checkoutRoot) {
  const pids = {}
  for (const name of peerNames) {
    pids[name] = Number(
      await readFile(path.join(checkoutRoot, `${name}.pid`), 'utf8')
    )
  }
  return pids
}

test('idle live owner restart stops owned children, holds the claim, and starts on the same allocation', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  writeRunPPeers(checkout.root)
  const owner = await claimSutOwnership(checkout.root)
  const logFile = path.join(checkout.root, 'sut.log')
  const foreign = spawnForeignProcess()
  const foreignListener = await listenTcp()
  const supervisor = spawn(
    process.execPath,
    [path.join(checkout.root, 'run-supervisor.mjs')],
    {
      cwd: checkout.root,
      env: {
        ...process.env,
        SUT_OWNER_TOKEN: owner.token,
        SUT_OWNER_CONTROL_PATH: owner.controlPath,
        SUT_CHECKOUT_ROOT: checkout.root,
        SUT_LOG_FILE: logFile,
      },
      stdio: 'ignore',
      detached: true,
    }
  )
  const state = { pids: {} }
  t.after(() => {
    try {
      supervisor.kill('SIGKILL')
    } catch {
      // already gone
    }
    for (const pid of Object.values(state.pids)) {
      if (!Number.isInteger(pid) || pid <= 0) continue
      try {
        process.kill(pid, 'SIGKILL')
      } catch {
        // already gone
      }
    }
    try {
      foreign.kill('SIGKILL')
    } catch {
      // already gone
    }
    closeServer(foreignListener.server)
  })

  await waitUntil(
    async () => (await verifyLiveSutOwner(checkout.root)).ok,
    5_000,
    'timed out waiting for live SUT owner'
  )
  state.pids = await waitUntil(
    async () => {
      try {
        const pids = await readPeerPids(checkout.root)
        if (
          peerNames.every(
            (name) => Number.isInteger(pids[name]) && pids[name] > 0
          )
        ) {
          return pids
        }
      } catch {
        // pid file not written yet
      }
      return false
    },
    5_000,
    'timed out waiting for run-p peer pids'
  )
  const supervisorPid = supervisor.pid
  const start = makeStartSpy()
  const code = await runSutRestart({
    checkoutRoot: checkout.root,
    execFileFn: lsofMustNotRun(),
    spawnFn: start.spawnFn,
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
