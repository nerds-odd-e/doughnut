import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  assertOwnedMockReleasedAndPeerServing,
  spawnIdleMockRunnerBoundary,
  waitForRunnerExit,
} from './isolated-cypress-idle-mock-runner-fixtures.mjs'

test('owned mock exit after startup fails the runner, releases its lease, and leaves a peer responding', async (t) => {
  const peerBody = 'peer-mock-still-serving'
  const { checkout, mockPid, runner, peer } = await spawnIdleMockRunnerBoundary(
    t,
    peerBody
  )

  // Startup already resolved (ready file). Kill only after that so this proof
  // fails if observation exists solely inside the startup promise.
  process.kill(mockPid, 'SIGKILL')

  const exit = await waitForRunnerExit(
    runner,
    5_000,
    'runner did not exit after owned mock failure'
  )
  assert.ok(
    exit.code !== 0 || exit.signal,
    `expected failed runner, got code=${exit.code} signal=${exit.signal}`
  )
  await assertOwnedMockReleasedAndPeerServing({
    checkoutRoot: checkout.root,
    mockPid,
    peer,
    peerBody,
  })
})
