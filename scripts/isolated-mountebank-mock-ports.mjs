/**
 * Port allocation for a runner-owned private Mountebank mock.
 *
 * `allocateMountebankMockPorts` is the generic core: it reserves one
 * management port and one serving port, excluding the canonical Mountebank
 * management port, a caller-supplied set of canonical serving ports (one per
 * mocked service), and the checkout's application ports. OpenAI (and, later,
 * Wikidata) wrap it with their own canonical serving port.
 *
 * Excludes shared defaults (2525/5001) and the checkout's application ports.
 */
import {
  closeListeningServer,
  listenEphemeralPort,
} from './sut-e2e-port-listen.mjs'

export const SHARED_MOUNTEBANK_MANAGEMENT_PORT = 2525
export const SHARED_OPEN_AI_SERVING_PORT = 5001
export const SHARED_WIKIDATA_SERVING_PORT = 5002

const ALLOCATE_ATTEMPTS = 24

function excludedApplicationPorts(allocation) {
  const used = new Set()
  const e2e = allocation?.e2e
  if (e2e && typeof e2e === 'object') {
    for (const field of ['backendPort', 'vitePort', 'lbListenPort']) {
      if (Number.isInteger(e2e[field])) used.add(e2e[field])
    }
  }
  return used
}

/**
 * Reserve one management port and one serving port until the mock binds.
 * Excludes the canonical Mountebank management port,
 * the caller's canonical serving ports, and the checkout's application ports.
 * Delegates to the multi-service allocator with `servingPortCount: 1` and
 * adapts the return shape to the single-service contract.
 */
export async function allocateMountebankMockPorts(
  allocation,
  { excludeServingPorts = [] } = {}
) {
  const { servingPorts, ...reservation } =
    await allocateMountebankMockPortsMulti(allocation, {
      excludeServingPorts,
      servingPortCount: 1,
    })
  return { ...reservation, servingPort: servingPorts[0] }
}

/**
 * Multi-service allocation: reserve one management port and `servingPortCount`
 * serving ports (one per mocked service). The starter releases each port
 * just before binding and releases any unused reservations when startup ends.
 * Excludes the canonical Mountebank management port, the
 * caller's canonical serving ports (one per mocked service), and the
 * checkout's application ports. Used when one invocation requires more than
 * one mocked service under a single owned management process; the
 * single-service allocator wraps this with `servingPortCount: 1`.
 */
export async function allocateMountebankMockPortsMulti(
  allocation,
  { excludeServingPorts = [], servingPortCount = 2 } = {}
) {
  const held = new Map()
  async function releasePort(port) {
    const server = held.get(port)
    if (server) {
      held.delete(port)
      await closeListeningServer(server)
    }
  }
  async function releasePorts() {
    await Promise.all([...held.keys()].map(releasePort))
  }
  const used = new Set([SHARED_MOUNTEBANK_MANAGEMENT_PORT])
  for (const port of excludeServingPorts) {
    if (Number.isInteger(port)) used.add(port)
  }
  for (const port of excludedApplicationPorts(allocation)) {
    used.add(port)
  }
  const target = 1 + servingPortCount
  try {
    let attempts = 0
    while (held.size < target) {
      attempts += 1
      if (attempts > ALLOCATE_ATTEMPTS) {
        throw new Error(
          `Unable to allocate free private Mountebank mock management and ${servingPortCount} serving ports.`
        )
      }
      const reserved = await listenEphemeralPort()
      if (used.has(reserved.port)) {
        await closeListeningServer(reserved.server)
        continue
      }
      used.add(reserved.port)
      held.set(reserved.port, reserved.server)
    }
    const [managementPort, ...servingPorts] = held.keys()
    return { managementPort, servingPorts, releasePort, releasePorts }
  } catch (error) {
    await releasePorts()
    throw error
  }
}

/**
 * OpenAI-specific allocation: exclude the OpenAI canonical serving port (5001)
 * in addition to the canonical management port and the checkout's app ports.
 */
export async function allocatePrivateOpenAiMockPorts(allocation) {
  return allocateMountebankMockPorts(allocation, {
    excludeServingPorts: [SHARED_OPEN_AI_SERVING_PORT],
  })
}

/**
 * Wikidata-specific allocation: exclude the Wikidata canonical serving port
 * (5002) in addition to the canonical management port and the checkout's app
 * ports. Wikidata's adapter consumes this owned endpoint instead of the
 * hardcoded 5002/default 2525.
 */
export async function allocatePrivateWikidataMockPorts(allocation) {
  return allocateMountebankMockPorts(allocation, {
    excludeServingPorts: [SHARED_WIKIDATA_SERVING_PORT],
  })
}
