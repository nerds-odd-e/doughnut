import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  closeServer,
  completeIsolatedConfig,
  isPidAlive,
  listenHttpReady,
  startLiveOwner,
  waitForFile,
} from './sut-isolated-fixtures.mjs'
import { acquireSutRunnerLease } from './sut-owner.mjs'
import { waitUntil } from './sut-owned-supervisor-fixtures.mjs'

export const idleMockRunnerPath = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'isolated-cypress-idle-mock-runner.mjs'
)

/** Checkout + live owner + peer + spawned idle-mock runner past ready. */
export async function spawnIdleMockRunnerBoundary(t, peerBody) {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  await startLiveOwner(checkout.root, t)

  const peer = await listenHttpReady(peerBody)
  t.after(() => closeServer(peer.server))

  const readyFile = path.join(checkout.root, 'mock-runner-ready.json')
  const runner = spawn(process.execPath, [idleMockRunnerPath], {
    cwd: checkout.root,
    env: {
      ...process.env,
      CHECKOUT_ROOT: checkout.root,
      READY_FILE: readyFile,
    },
    stdio: ['ignore', 'ignore', 'pipe'],
  })
  t.after(() => {
    try {
      runner.kill('SIGKILL')
    } catch {
      // already gone
    }
  })

  await waitForFile(readyFile, 10_000)
  const { mockPid } = JSON.parse(await readFile(readyFile, 'utf8'))
  assert.equal(isPidAlive(mockPid), true)
  assert.equal(isPidAlive(runner.pid), true)

  return { checkout, mockPid, runner, peer }
}

export async function waitForRunnerExit(runner, timeoutMs, message) {
  return waitUntil(
    async () => {
      if (runner.exitCode !== null || runner.signalCode !== null) {
        return { code: runner.exitCode, signal: runner.signalCode }
      }
      return false
    },
    timeoutMs,
    message
  )
}

export async function assertOwnedMockReleasedAndPeerServing({
  checkoutRoot,
  mockPid,
  peer,
  peerBody,
}) {
  assert.equal(isPidAlive(mockPid), false)

  const nextLease = await acquireSutRunnerLease(checkoutRoot)
  assert.ok(typeof nextLease === 'string' && nextLease.length > 0)

  assert.equal(
    await (await fetch(`http://127.0.0.1:${peer.port}/`)).text(),
    peerBody
  )
}
