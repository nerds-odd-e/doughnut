import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  assertOwnedMockReleasedAndPeerServing,
  spawnIdleMockRunnerBoundary,
  waitForRunnerExit,
} from './isolated-cypress-idle-mock-runner-fixtures.mjs'

for (const cancelSignal of ['SIGINT', 'SIGTERM']) {
  test(`cancel (${cancelSignal}) stops owned mock before lease reuse and leaves a peer responding`, async (t) => {
    const peerBody = `peer-still-serving-after-${cancelSignal}`
    const { checkout, mockPid, runner, peer } =
      await spawnIdleMockRunnerBoundary(t, peerBody)

    runner.kill(cancelSignal)

    const exit = await waitForRunnerExit(
      runner,
      5_000,
      `runner did not exit after ${cancelSignal}`
    )
    assert.ok(
      exit.code !== 0 || exit.signal,
      `expected canceled runner, got code=${exit.code} signal=${exit.signal}`
    )
    await assertOwnedMockReleasedAndPeerServing({
      checkoutRoot: checkout.root,
      mockPid,
      peer,
      peerBody,
    })
  })
}
