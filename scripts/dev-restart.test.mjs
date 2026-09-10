import { writeFileSync } from 'node:fs'
import { test } from 'node:test'
import { makePrimaryCheckout } from './backend-test-worktree-linked-fixtures.mjs'
import {
  assertRefuseWithoutSignalOrStart,
  listenersForPorts,
  makeLinkedWorktreeCheckout,
  makeStartSpy,
  occupiedPrimaryPorts,
  targetFor,
  trackingKill,
  withRefuseDeps,
} from './dev-restart-fixtures.mjs'
import {
  allocateFreePort,
  closeServer,
  listenTcp,
} from './sut-isolated-fixtures.mjs'

test('occupied Development ports with missing dev.pid refuse without signalling or starting', async (t) => {
  const { checkout, port, runtimeTarget } = await occupiedPrimaryPorts(t)
  const kill = trackingKill()
  const start = makeStartSpy()

  await assertRefuseWithoutSignalOrStart(
    withRefuseDeps({
      checkoutRoot: checkout.root,
      runtimeTarget,
      kill,
      start,
      getListenerPidsFn: listenersForPorts(new Map([[port, [55501]]])),
    }),
    /missing|Refusing restart/i,
    { kill, start, listeningPorts: [port] }
  )
})

test('occupied Development ports with stale dev.pid refuse without signalling or starting', async (t) => {
  const { checkout, port, runtimeTarget } = await occupiedPrimaryPorts(t)
  writeFileSync(runtimeTarget.pidFile, '424242')
  const kill = trackingKill()
  const start = makeStartSpy()

  await assertRefuseWithoutSignalOrStart(
    withRefuseDeps({
      checkoutRoot: checkout.root,
      runtimeTarget,
      kill,
      start,
      isProcessAliveFn: () => false,
      getListenerPidsFn: listenersForPorts(new Map([[port, [55502]]])),
    }),
    /stale|Refusing restart/i,
    { kill, start, listeningPorts: [port] }
  )
})

test('live dev.pid with foreign listener refuses without signalling the foreign process', async (t) => {
  const { checkout, port, runtimeTarget } = await occupiedPrimaryPorts(t)
  writeFileSync(runtimeTarget.pidFile, String(process.pid))
  const kill = trackingKill()
  const start = makeStartSpy()

  await assertRefuseWithoutSignalOrStart(
    withRefuseDeps({
      checkoutRoot: checkout.root,
      runtimeTarget,
      kill,
      start,
      getListenerPidsFn: listenersForPorts(new Map([[port, [77701]]])),
      isOwnedByApplicationTreeFn: async () => false,
    }),
    /not owned|Refusing restart/i,
    { kill, start, listeningPorts: [port] }
  )
})

test('incomplete ownership (owned + foreign listeners) refuses without signalling either', async (t) => {
  const checkout = makePrimaryCheckout(t)
  const backend = await listenTcp()
  t.after(() => closeServer(backend.server))
  const vite = await listenTcp()
  t.after(() => closeServer(vite.server))
  const runtimeTarget = targetFor(checkout.root, {
    backendPort: backend.port,
    vitePort: vite.port,
    lbListenPort: await allocateFreePort(),
  })
  writeFileSync(runtimeTarget.pidFile, String(process.pid))
  const ownedPid = 88801
  const kill = trackingKill()
  const start = makeStartSpy()

  await assertRefuseWithoutSignalOrStart(
    withRefuseDeps({
      checkoutRoot: checkout.root,
      runtimeTarget,
      kill,
      start,
      getListenerPidsFn: listenersForPorts(
        new Map([
          [backend.port, [ownedPid]],
          [vite.port, [88802]],
        ])
      ),
      isOwnedByApplicationTreeFn: async (pid) => pid === ownedPid,
    }),
    /not owned|Refusing restart/i,
    { kill, start, listeningPorts: [backend.port, vite.port] }
  )
})

test('invalid non-numeric dev.pid with occupied ports refuses without signalling', async (t) => {
  const { checkout, port, runtimeTarget } = await occupiedPrimaryPorts(t)
  writeFileSync(runtimeTarget.pidFile, 'not-a-pid')
  const kill = trackingKill()
  const start = makeStartSpy()

  await assertRefuseWithoutSignalOrStart(
    withRefuseDeps({
      checkoutRoot: checkout.root,
      runtimeTarget,
      kill,
      start,
      getListenerPidsFn: listenersForPorts(new Map([[port, [55503]]])),
    }),
    /missing|unreadable|Refusing restart/i,
    { kill, start }
  )
})

test('linked worktree refuses Development restart without signalling', async (t) => {
  const checkout = makeLinkedWorktreeCheckout(t)
  const kill = trackingKill()
  const start = makeStartSpy()

  await assertRefuseWithoutSignalOrStart(
    withRefuseDeps({
      checkoutRoot: checkout.root,
      runtimeTarget: targetFor(checkout.root),
      kill,
      start,
      getListenerPidsFn: async () => {
        throw new Error('must not probe listeners for linked worktree')
      },
    }),
    /unconfigured primary|worktree isolation/,
    { kill, start }
  )
})
