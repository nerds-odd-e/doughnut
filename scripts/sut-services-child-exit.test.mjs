import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
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

import {
  peerNames,
  waitUntil,
  writeRunPPeers,
  writePersistentlyLiveSupervisor,
  readPeerPids,
} from './sut-services-child-exit-fixtures.mjs'

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

test('supervisor reports bounded cleanup failure and retains ownership', async (t) => {
  const checkout = makePrimaryCheckout(t)
  writeIsolatedConfig(checkout.root)
  writePersistentlyLiveSupervisor(checkout.root)
  const owner = await claimSutOwnership(checkout.root)
  const logFile = path.join(checkout.root, 'sut.log')
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
  t.after(() => {
    try {
      supervisor.kill('SIGKILL')
    } catch {
      // already gone
    }
  })

  const [code, signal] = await new Promise((resolve) => {
    supervisor.once('exit', (exitCode, exitSignal) =>
      resolve([exitCode, exitSignal])
    )
  })
  const log = await readFile(logFile, 'utf8')
  assert.equal(signal, null)
  assert.equal(code, 1)
  assert.match(
    log,
    /SUT supervisor cleanup failed: Owned SUT process tree did not exit after SIGKILL within the bounded wait/
  )
  assert.equal(existsSync(sutOwnerLockDir(checkout.root)), true)
})
