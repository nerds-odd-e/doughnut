import assert from 'node:assert/strict'
import test from 'node:test'
import {
  makeLinkedWorktreeCheckout,
  makePrimaryCheckout,
} from './backend-test-worktree-linked-fixtures.mjs'
import { startStandInDevelopmentStack } from './dev-stack-fixtures.mjs'
import { runDevStop } from './dev-stop.mjs'
import { isPidAlive, isTcpListening } from './sut-isolated-fixtures.mjs'

function captureLog() {
  const lines = []
  return { lines, log: (line) => lines.push(line) }
}

test('stop ends a running stack that no file names: processes gone, port free', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const { port, pids } = await startStandInDevelopmentStack(t, checkout.root)
  const { lines, log } = captureLog()

  assert.equal(await runDevStop({ checkoutRoot: checkout.root, log }), 0)

  for (const pid of Object.values(pids)) {
    assert.equal(isPidAlive(pid), false, `pid ${pid} still alive`)
  }
  assert.equal(await isTcpListening(port), false)
  assert.match(lines.join('\n'), new RegExp(`pid ${pids.services}`))
})

test('stop with nothing running says so and succeeds', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const { lines, log } = captureLog()

  assert.equal(await runDevStop({ checkoutRoot: checkout.root, log }), 0)

  assert.deepEqual(lines, ['Development is not running.'])
})

test("stop leaves another checkout's stack running", async (t) => {
  const checkout = makePrimaryCheckout(t)
  const other = makePrimaryCheckout(t)
  await startStandInDevelopmentStack(t, checkout.root)
  const { port, pids } = await startStandInDevelopmentStack(t, other.root)

  await runDevStop({ checkoutRoot: checkout.root, log: captureLog().log })

  for (const pid of Object.values(pids)) {
    assert.equal(isPidAlive(pid), true, `pid ${pid} was stopped`)
  }
  assert.equal(await isTcpListening(port), true)
})

test('linked worktree refuses stop', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)

  await assert.rejects(
    runDevStop({ checkoutRoot: checkout.root, log: captureLog().log }),
    /only supported in the primary checkout/
  )
})
