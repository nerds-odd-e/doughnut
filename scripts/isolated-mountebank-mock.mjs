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
import http from 'node:http'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { stopOwnedSutProcessTree } from './sut-owned-process-tree.mjs'
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

function mbBinary(checkoutRoot) {
  return path.join(checkoutRoot, 'node_modules', '.bin', 'mb')
}

function postJson(url, body) {
  return new Promise((resolve, reject) => {
    const payload = JSON.stringify(body)
    const target = new URL(url)
    const req = http.request(
      {
        hostname: target.hostname,
        port: target.port,
        path: target.pathname,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(payload),
        },
      },
      (res) => {
        const chunks = []
        res.on('data', (c) => chunks.push(c))
        res.on('end', () => {
          resolve({
            statusCode: res.statusCode ?? 0,
            body: Buffer.concat(chunks).toString('utf8'),
          })
        })
      }
    )
    req.on('error', reject)
    req.write(payload)
    req.end()
  })
}

/**
 * Create a recording imposter on `servingPort` through `managementUrl`.
 * The `serviceLabel` names the service in failure messages. When `stubs` is
 * supplied the imposter is created with those stubs; by default the imposter
 * is empty (records requests, serves no canned response).
 */
export async function createEmptyRecordingImposter(
  managementUrl,
  servingPort,
  { serviceLabel = 'mock', stubs = [] } = {}
) {
  const response = await postJson(`${managementUrl}/imposters`, {
    protocol: 'http',
    port: servingPort,
    recordRequests: true,
    stubs,
  })
  if (response.statusCode !== 201) {
    throw new Error(
      `Private ${serviceLabel} mock failed to create recording imposter on port ${servingPort}: ` +
        `status ${response.statusCode} ${response.body}`
    )
  }
}

/**
 * Failure observation + stop/kill for one owned private-mock child.
 * Used by the real starter and by the spawned mock-failure proof runner.
 * The `serviceLabel` names the service in failure messages.
 */
export function observeOwnedMockChild(child, { serviceLabel = 'mock' } = {}) {
  const failState = { error: null }
  const onChildGone = (code, signal) => {
    if (failState.error) return
    failState.error = new Error(
      `Private ${serviceLabel} mock process exited unexpectedly ` +
        `(code ${code ?? 'unknown'}, signal ${signal ?? 'none'}).`
    )
  }
  child.once('exit', onChildGone)
  child.once('error', (error) => {
    failState.error = new Error(
      `Private ${serviceLabel} mock failed to start: ${error.message}`
    )
  })

  let stopped = false
  const disarm = () => child.removeListener('exit', onChildGone)

  return {
    getFailure() {
      return failState.error
    },
    async stop() {
      if (stopped) return
      stopped = true
      disarm()
      await stopOwnedSutProcessTree(child)
    },
    killSync() {
      if (stopped) return
      stopped = true
      disarm()
      const pgid = child.pid
      if (!Number.isInteger(pgid) || pgid <= 0) return
      try {
        process.kill(-pgid, 'SIGKILL')
      } catch (error) {
        if (error.code !== 'ESRCH' && error.code !== 'EPERM') throw error
      }
    },
  }
}

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
      const { managementPort, servingPort } = await allocatePortsFn(a)
      return { managementPort, servingPorts: [servingPort] }
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
 * management listener. The caller supplies the port allocator
 * (`allocatePortsFn`, returning `{ managementPort, servingPorts: [...] }`)
 * and `serviceConfigs` (one per serving port, each carrying a `serviceLabel`
 * and optional `stubs`). The management listener is verified under a shared
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
  const { managementPort, servingPorts } = await allocatePortsFn(allocation)
  if (
    !Array.isArray(servingPorts) ||
    servingPorts.length !== serviceConfigs.length
  ) {
    throw new Error(
      `startOwnedMountebankMockMulti: allocatePortsFn must return servingPorts matching serviceConfigs length (${serviceConfigs.length}).`
    )
  }
  const managementUrl = `http://127.0.0.1:${managementPort}`
  const pidfile = path.join(
    checkoutRoot,
    `.isolated-mb.${process.pid}.${managementPort}.pid`
  )

  const child = spawnFn(
    mbBinary(checkoutRoot),
    [
      'start',
      '--port',
      String(managementPort),
      '--pidfile',
      pidfile,
      '--nologfile',
      '--loglevel',
      'error',
    ],
    {
      cwd: checkoutRoot,
      detached: true,
      stdio: 'ignore',
      shell: false,
    }
  )
  child.unref()

  const lifecycle = observeOwnedMockChild(child, managementOpts)
  const throwIfMockFailed = () => {
    const failure = lifecycle.getFailure()
    if (failure) throw failure
  }

  const endpoints = []
  try {
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
  } catch (error) {
    await lifecycle.stop()
    throw error
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
}
