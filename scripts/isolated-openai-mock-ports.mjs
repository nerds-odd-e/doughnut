/**
 * Temporary management + OpenAI serving ports for a runner-owned private mock.
 * Excludes shared defaults (2525/5001) and the checkout's application ports.
 */
import {
  closeListeningServer,
  listenEphemeralPort,
} from './sut-e2e-port-listen.mjs'

export const SHARED_MOUNTEBANK_MANAGEMENT_PORT = 2525
export const SHARED_OPEN_AI_SERVING_PORT = 5001

const ALLOCATE_ATTEMPTS = 24

function excludedMockPorts(allocation) {
  const used = new Set([
    SHARED_MOUNTEBANK_MANAGEMENT_PORT,
    SHARED_OPEN_AI_SERVING_PORT,
  ])
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
 * Reserve two free ports, then release the holds so the mock child can bind.
 * @returns {Promise<{ managementPort: number, servingPort: number }>}
 */
export async function allocatePrivateOpenAiMockPorts(allocation) {
  const held = []
  const used = new Set(excludedMockPorts(allocation))
  try {
    let attempts = 0
    while (held.length < 2) {
      attempts += 1
      if (attempts > ALLOCATE_ATTEMPTS) {
        throw new Error(
          'Unable to allocate free private OpenAI mock management and serving ports.'
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
