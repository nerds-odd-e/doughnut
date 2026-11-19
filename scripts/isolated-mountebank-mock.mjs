/**
 * Generic runner-owned private Mountebank lifecycle for one isolated Cypress
 * spec that requires a mocked external service. This is the service-label-
 * parameterized ownership adapter extracted from the OpenAI-specific starter:
 * it owns the Mountebank child process, the management listener readiness
 * check, the serving-port exclusion, the empty recording imposter, and the
 * verification/stop/kill behavior — independent of any one service's endpoint
 * name. OpenAI (and, later, Wikidata) consume this adapter by supplying their
 * own service label, port allocator, and excluded serving ports.
 *
 * Does not call start_mb.sh (shared-port adoption / swallowed failure).
 */
import { spawn } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { createEmptyRecordingImposter } from './isolated-mountebank-imposter.mjs'
import {
  observeOwnedMockChild,
  spawnOwnedMockChild,
} from './isolated-mountebank-mock-process.mjs'
import {
  assertOwnedMockListener,
  assertPortFreeBeforeMockMutation,
  ownedProcessGroupId,
  waitForOwnedManagementListener,
} from './isolated-mountebank-mock-ownership.mjs'

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..'
)

export { createEmptyRecordingImposter, observeOwnedMockChild }

/**
 * Start one owned Mountebank management process and a single empty recording
 * imposter on a serving port. The caller supplies the port allocator
 * (`allocatePortsFn`, returning `{ managementPort, servingPort }`) and the
 * `serviceLabel` used in ownership/failure messages; the allocator is
 * responsible for excluding that service's canonical serving port and the
 * checkout's application ports.
 *
 * Delegates to `startOwnedMountebankMockMulti` with one service config and
 * adapts the return shape to the single-service contract (`endpoint` rather
 * than `endpoints`).
 *
 * @returns {Promise<{
 *   endpoint: { managementUrl: string, servingPort: number },
 *   child: import('node:child_process').ChildProcess,
 *   verifyOwnership: () => Promise<true>,
 *   stop: () => Promise<void>,
 *   killSync: () => void,
 *   getFailure: () => Error | null,
 * }>}
 */
export async function startOwnedMountebankMock({
  checkoutRoot = repoRoot,
  allocation,
  spawnFn = spawn,
  allocatePortsFn,
  serviceLabel = 'mock',
} = {}) {
  if (typeof allocatePortsFn !== 'function') {
    throw new Error(
      `startOwnedMountebankMock requires an allocatePortsFn (service: ${serviceLabel}).`
    )
  }
  const multi = await startOwnedMountebankMockMulti({
    checkoutRoot,
    allocation,
    spawnFn,
    allocatePortsFn: async (a) => {
      const { servingPort, ...reservation } = await allocatePortsFn(a)
      return { ...reservation, servingPorts: [servingPort] }
    },
    serviceConfigs: [{ serviceLabel }],
    managementLabel: serviceLabel,
  })
  const [endpoint] = multi.endpoints
  return {
    endpoint: {
      managementUrl: endpoint.managementUrl,
      servingPort: endpoint.servingPort,
    },
    child: multi.child,
    verifyOwnership: multi.verifyOwnership,
    stop: multi.stop,
    killSync: multi.killSync,
    getFailure: multi.getFailure,
  }
}

/**
 * Start one owned Mountebank management process and one recording imposter
 * per service config, each on its own serving port, all under a single owned
 * management listener. The caller supplies a port allocator and service
 * configs. A private allocator can supply release callbacks for held ports.
 * Each service config supplies a label and optional stubs. The management
 * listener is verified under a shared
 * `managementLabel`; each serving listener is verified under its own service
 * label. Failure/cancellation settles the one process and all partial
 * allocations without touching a peer.
 *
 * @returns {Promise<{
 *   endpoints: { managementUrl: string, servingPort: number, serviceLabel: string }[],
 *   child: import('node:child_process').ChildProcess,
 *   verifyOwnership: () => Promise<true>,
 *   stop: () => Promise<void>,
 *   killSync: () => void,
 *   getFailure: () => Error | null,
 * }>}
 */
export async function startOwnedMountebankMockMulti({
  checkoutRoot = repoRoot,
  allocation,
  spawnFn = spawn,
  allocatePortsFn,
  serviceConfigs,
  managementLabel = 'multi-mock',
} = {}) {
  if (!Array.isArray(serviceConfigs) || serviceConfigs.length === 0) {
    throw new Error(
      'startOwnedMountebankMockMulti requires a non-empty serviceConfigs array.'
    )
  }
  if (typeof allocatePortsFn !== 'function') {
    throw new Error(
      `startOwnedMountebankMockMulti requires an allocatePortsFn (management label: ${managementLabel}).`
    )
  }
  const managementOpts = { serviceLabel: managementLabel }
  const { managementPort, servingPorts, releasePort, releasePorts } =
    await allocatePortsFn(allocation)
  let lifecycle
  try {
    if (
      !Array.isArray(servingPorts) ||
      servingPorts.length !== serviceConfigs.length
    ) {
      throw new Error(
        `startOwnedMountebankMockMulti: allocatePortsFn must return servingPorts matching serviceConfigs length (${serviceConfigs.length}).`
      )
    }
    const managementUrl = `http://127.0.0.1:${managementPort}`
    await releasePort?.(managementPort)
    const child = spawnOwnedMockChild(checkoutRoot, managementPort, spawnFn)
    lifecycle = observeOwnedMockChild(child, managementOpts)
    child.unref()

    const throwIfMockFailed = () => {
      const failure = lifecycle.getFailure()
      if (failure) throw failure
    }
    const endpoints = []
    await waitForOwnedManagementListener(child, managementPort, managementOpts)
    throwIfMockFailed()
    await assertOwnedMockListener(
      managementPort,
      ownedProcessGroupId(child, managementOpts),
      'management',
      managementOpts
    )
    for (let i = 0; i < serviceConfigs.length; i++) {
      const { serviceLabel, stubs = [] } = serviceConfigs[i]
      const servingPort = servingPorts[i]
      const serviceOpts = { serviceLabel }
      await releasePort?.(servingPort)
      await assertPortFreeBeforeMockMutation(
        servingPort,
        'serving',
        serviceOpts
      )
      throwIfMockFailed()
      await createEmptyRecordingImposter(managementUrl, servingPort, {
        serviceLabel,
        stubs,
      })
      throwIfMockFailed()
      await assertOwnedMockListener(
        servingPort,
        ownedProcessGroupId(child, managementOpts),
        'serving',
        serviceOpts
      )
      throwIfMockFailed()
      endpoints.push({ managementUrl, servingPort, serviceLabel })
    }

    const verifyOwnership = async () => {
      throwIfMockFailed()
      const ownedPgid = ownedProcessGroupId(child, managementOpts)
      await assertOwnedMockListener(
        managementPort,
        ownedPgid,
        'management',
        managementOpts
      )
      for (const endpoint of endpoints) {
        await assertOwnedMockListener(
          endpoint.servingPort,
          ownedPgid,
          'serving',
          { serviceLabel: endpoint.serviceLabel }
        )
      }
      throwIfMockFailed()
      return true
    }

    return {
      endpoints,
      child,
      verifyOwnership,
      stop: lifecycle.stop,
      killSync: lifecycle.killSync,
      getFailure: lifecycle.getFailure,
    }
  } catch (error) {
    await lifecycle?.stop()
    throw error
  } finally {
    await releasePorts?.()
  }
}
