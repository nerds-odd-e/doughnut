import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { existsSync, writeFileSync } from 'node:fs'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import { runSutHealthcheck } from './sut-healthcheck.mjs'
import {
  closeServer,
  isPidAlive,
  isTcpListening,
  listenTcp,
  spawnForeignProcess,
  writeIsolatedConfig,
} from './sut-isolated-fixtures.mjs'
import {
  claimSutOwnership,
  sutOwnerLockDir,
  verifyLiveSutOwner,
} from './sut-owner.mjs'

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

function writeRunPPeers(checkoutRoot) {
  writeFileSync(
    path.join(checkoutRoot, 'package.json'),
    `${JSON.stringify({
      name: 'sut-supervisor-child-exit-fixture',
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
import { runSutServices, sutServiceArgs } from ${JSON.stringify(
      path.join(worktreeRoot, 'scripts/sut-services.mjs')
    )}
import { startSutOwnerControlFromEnv } from ${JSON.stringify(
      path.join(worktreeRoot, 'scripts/sut-owner.mjs')
    )}

await startSutOwnerControlFromEnv()
runSutServices({
  checkoutRoot: process.env.SUT_CHECKOUT_ROOT,
  logFile: process.env.SUT_LOG_FILE,
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

test('healthy supervisor logs a forced child exit and releases owned peers', async (t) => {
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
  for (const name of peerNames) {
    assert.equal(isPidAlive(state.pids[name]), true)
  }

  process.kill(state.pids.backend, 'SIGKILL')

  const log = await waitUntil(
    async () => {
      try {
        const content = await readFile(logFile, 'utf8')
        return /Forced SUT service child exit/.test(content) ? content : false
      } catch {
        return false
      }
    },
    5_000,
    'timed out waiting for forced child-exit log'
  )
  await waitUntil(
    () =>
      !(
        isPidAlive(state.pids.lb) ||
        isPidAlive(state.pids.frontend) ||
        isPidAlive(supervisor.pid)
      ),
    5_000,
    'timed out waiting for owned peers and supervisor to exit'
  )
  assert.match(log, /Forced SUT service child exit/)
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), false)
  assert.equal((await verifyLiveSutOwner(checkout.root)).ok, false)
  const health = await runSutHealthcheck({
    checkoutRoot: checkout.root,
    log: () => undefined,
  })
  assert.equal(health.ok, false)
  assert.equal(isPidAlive(foreign.pid), true)
  assert.equal(await isTcpListening(foreignListener.port), true)
})
