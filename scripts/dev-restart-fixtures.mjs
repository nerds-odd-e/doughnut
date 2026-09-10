import assert from 'node:assert/strict'
import path from 'node:path'
import {
  makeLinkedWorktreeCheckout,
  makePrimaryCheckout,
} from './backend-test-worktree-linked-fixtures.mjs'
import { DEVELOPMENT_RUNTIME_TARGET } from './development-runtime.mjs'
import { runDevRestart } from './dev-restart.mjs'
import {
  allocateFreePort,
  closeServer,
  isTcpListening,
  listenTcp,
} from './sut-isolated-fixtures.mjs'
import { makeStartSpy } from './sut-start-fixtures.mjs'

export function targetFor(checkoutRoot, ports = {}) {
  return {
    ...DEVELOPMENT_RUNTIME_TARGET,
    ...ports,
    logFile: path.join(checkoutRoot, 'dev.log'),
    pidFile: path.join(checkoutRoot, 'dev.pid'),
  }
}

export function trackingKill() {
  const calls = []
  return {
    calls,
    killFn(pid, signal) {
      calls.push({ pid, signal })
      return true
    },
  }
}

export function listenersForPorts(map) {
  return async (port) => map.get(port) ?? []
}

export async function occupiedPrimaryPorts(t) {
  const checkout = makePrimaryCheckout(t)
  const { server, port } = await listenTcp()
  t.after(() => closeServer(server))
  const vitePort = await allocateFreePort()
  const lbListenPort = await allocateFreePort()
  return {
    checkout,
    port,
    runtimeTarget: targetFor(checkout.root, {
      backendPort: port,
      vitePort,
      lbListenPort,
    }),
  }
}

export async function assertRefuseWithoutSignalOrStart(
  run,
  pattern,
  { kill, start, listeningPorts = [] }
) {
  await assert.rejects(run, pattern)
  assert.equal(kill.calls.length, 0)
  assert.equal(start.calls.length, 0)
  for (const port of listeningPorts) {
    assert.equal(await isTcpListening(port), true)
  }
}

export function withRefuseDeps({
  checkoutRoot,
  runtimeTarget,
  kill,
  start,
  getListenerPidsFn,
  isProcessAliveFn,
  isOwnedByApplicationTreeFn = async () => true,
}) {
  return runDevRestart({
    checkoutRoot,
    runtimeTarget,
    getListenerPidsFn,
    isProcessAliveFn,
    isOwnedByApplicationTreeFn,
    killFn: kill.killFn,
    runDevStartFn: async () => {
      start.calls.push(['runDevStart'])
      return 0
    },
  })
}

export {
  allocateFreePort,
  closeServer,
  listenTcp,
  makeLinkedWorktreeCheckout,
  makePrimaryCheckout,
  makeStartSpy,
}
