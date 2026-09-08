import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'
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

const mockFailureRunner = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  'isolated-cypress-mock-failure-runner.mjs'
)

test('owned mock exit after startup fails the runner, releases its lease, and leaves a peer responding', async (t) => {
  const checkout = makePrimaryCheckout(t, {
    config: JSON.stringify(completeIsolatedConfig),
  })
  const live = await startLiveOwner(checkout.root)
  t.after(() => live.server.close())

  const peerBody = 'peer-mock-still-serving'
  const peer = await listenHttpReady(peerBody)
  t.after(() => closeServer(peer.server))

  const readyFile = path.join(checkout.root, 'mock-runner-ready.json')
  const runner = spawn(process.execPath, [mockFailureRunner], {
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

  // Startup already resolved (ready file). Kill only after that so this proof
  // fails if observation exists solely inside the startup promise.
  process.kill(mockPid, 'SIGKILL')

  const exit = await waitUntil(
    async () => {
      if (runner.exitCode !== null || runner.signalCode !== null) {
        return { code: runner.exitCode, signal: runner.signalCode }
      }
      return false
    },
    5_000,
    'runner did not exit after owned mock failure'
  )
  assert.ok(
    exit.code !== 0 || exit.signal,
    `expected failed runner, got code=${exit.code} signal=${exit.signal}`
  )
  assert.equal(isPidAlive(mockPid), false)

  const nextLease = await acquireSutRunnerLease(checkout.root)
  assert.ok(typeof nextLease === 'string' && nextLease.length > 0)

  assert.equal(
    await (await fetch(`http://127.0.0.1:${peer.port}/`)).text(),
    peerBody
  )
})
