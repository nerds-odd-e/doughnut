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

async function closeHeld(held) {
  for (const item of held) {
    await closeListeningServer(item.server)
  }
}

/**
 * Reserve one management port and one serving port, then release the holds so
 * the mock child can bind. Excludes the canonical Mountebank management port,
 * the caller's canonical serving ports, and the checkout's application ports.
 * @returns {Promise<{ managementPort: number, servingPort: number }>}
 */
export async function allocateMountebankMockPorts(
  allocation,
  { excludeServingPorts = [] } = {}
) {
  const held = []
  const used = new Set([SHARED_MOUNTEBANK_MANAGEMENT_PORT])
  for (const port of excludeServingPorts) {
    if (Number.isInteger(port)) used.add(port)
  }
  for (const port of excludedApplicationPorts(allocation)) {
    used.add(port)
  }
  try {
    let attempts = 0
    while (held.length < 2) {
      attempts += 1
      if (attempts > ALLOCATE_ATTEMPTS) {
        throw new Error(
          'Unable to allocate free private Mountebank mock management and serving ports.'
        )
      }
      const reserved = await listenEphemeralPort()
      if (used.has(reserved.port)) {
        await closeListeningServer(reserved.server)
        continue
      }
      used.add(reserved.port)
      held.push(reserved)
    }
    const managementPort = held[0].port
    const servingPort = held[1].port
    await closeHeld(held)
    return { managementPort, servingPort }
  } catch (error) {
    await closeHeld(held)
    throw error
  }
}

/**
 * OpenAI-specific allocation: exclude the OpenAI canonical serving port (5001)
 * in addition to the canonical management port and the checkout's app ports.
 * @returns {Promise<{ managementPort: number, servingPort: number }>}
 */
export async function allocatePrivateOpenAiMockPorts(allocation) {
  return allocateMountebankMockPorts(allocation, {
    excludeServingPorts: [SHARED_OPEN_AI_SERVING_PORT],
  })
}
