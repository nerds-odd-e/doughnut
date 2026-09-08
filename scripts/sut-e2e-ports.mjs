import { readFileSync, writeFileSync } from 'node:fs'
import {
  defaultE2ePortClaimRoot,
  readPublishedE2ePortClaims,
  withE2ePortClaimLock,
  writePublishedE2ePortClaims,
} from './sut-e2e-port-claims.mjs'
import {
  closeListeningServer,
  listenEphemeralPort,
} from './sut-e2e-port-listen.mjs'
import {
  assertValidWorktreeId,
  worktreeLocalConfigPath,
} from './worktree-identity.mjs'

export {
  readPublishedE2ePortClaims,
  withE2ePortClaimLock,
} from './sut-e2e-port-claims.mjs'

export const E2E_PORT_FIELDS = ['backendPort', 'vitePort', 'lbListenPort']
export const E2E_PORT_CONFIG_FIELDS = E2E_PORT_FIELDS.map(
  (field) => `e2e.${field}`
)
export const RESERVED_ISOLATED_E2E_PORTS = [5173, 5174, 9081, 2525]
const ALLOCATE_ATTEMPTS = 24

export function isRecordedE2eApplicationPort(port) {
  return Number.isInteger(port) && port >= 1 && port <= 65535
}

export function collectMissingE2ePorts(e2e) {
  if (!e2e || typeof e2e !== 'object') {
    return [...E2E_PORT_CONFIG_FIELDS]
  }
  const missing = []
  for (const field of E2E_PORT_FIELDS) {
    if (!isRecordedE2eApplicationPort(e2e[field])) {
      missing.push(`e2e.${field}`)
    }
  }
  if (missing.length === 0) {
    const values = E2E_PORT_FIELDS.map((field) => e2e[field])
    if (new Set(values).size !== values.length) {
      return [...E2E_PORT_CONFIG_FIELDS]
    }
  }
  return missing
}

export function collectPresentInvalidE2ePortFields(e2e) {
  if (!e2e || typeof e2e !== 'object') return []
  if (!E2E_PORT_FIELDS.some((field) => Object.hasOwn(e2e, field))) {
    return []
  }
  return collectMissingE2ePorts(e2e)
}

export function refusePresentInvalidIsolatedE2ePorts(checkoutRoot, e2e) {
  const missing = collectPresentInvalidE2ePortFields(e2e)
  if (missing.length > 0) {
    throw isolatedE2ePortsRequiredError(checkoutRoot, missing)
  }
}

function recordedE2eApplicationPorts(e2e) {
  return Object.fromEntries(E2E_PORT_FIELDS.map((field) => [field, e2e[field]]))
}

export function isolatedE2ePortsRequiredError(checkoutRoot, missing) {
  return new Error(
    `Isolated worktree SUT needs identity and application ports in ${worktreeLocalConfigPath(
      checkoutRoot
    )} (id, ${E2E_PORT_CONFIG_FIELDS.join(', ')}). ` +
      `Missing or invalid: ${missing.join(', ')}. See docs/worktree-browser-tests.md.`
  )
}

export function refusePartialIsolatedE2ePorts(checkoutRoot, e2e) {
  const missing = collectMissingE2ePorts(e2e)
  if (missing.length > 0 && missing.length < E2E_PORT_FIELDS.length) {
    throw isolatedE2ePortsRequiredError(checkoutRoot, missing)
  }
  return missing
}

function readCheckoutConfig(checkoutRoot) {
  const configPath = worktreeLocalConfigPath(checkoutRoot)
  const config = JSON.parse(readFileSync(configPath, 'utf8'))
  if (typeof config.id !== 'string') {
    throw new Error('Worktree configuration must contain a string "id".')
  }
  assertValidWorktreeId(config.id)
  return config
}

function recordCheckoutPorts(checkoutRoot, config, ports) {
  const next = {
    ...config,
    e2e: {
      ...(config.e2e && typeof config.e2e === 'object' ? config.e2e : {}),
      ...ports,
    },
  }
  writeFileSync(worktreeLocalConfigPath(checkoutRoot), JSON.stringify(next))
  return next
}

function claimedPortSet(claims) {
  const used = new Set(RESERVED_ISOLATED_E2E_PORTS)
  for (const ports of Object.values(claims)) {
    if (!ports || typeof ports !== 'object') continue
    for (const field of E2E_PORT_FIELDS) {
      if (Number.isInteger(ports[field])) used.add(ports[field])
    }
  }
  return used
}

async function closeHeldServers(held) {
  for (const item of held) {
    await closeListeningServer(item.server)
  }
}

async function reserveUnclaimedApplicationPorts(used) {
  const held = []
  try {
    let attempts = 0
    while (held.length < E2E_PORT_FIELDS.length) {
      attempts += 1
      if (attempts > ALLOCATE_ATTEMPTS) {
        throw new Error(
          'Unable to allocate three free isolated E2E application ports.'
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
    return held
  } catch (error) {
    await closeHeldServers(held)
    throw error
  }
}

function publishClaim(claimRoot, worktreeId, ports) {
  const claims = readPublishedE2ePortClaims(claimRoot)
  claims[worktreeId] = ports
  writePublishedE2ePortClaims(claimRoot, claims)
}

async function publishOrAllocatePorts(claimRoot, checkoutRoot) {
  const config = readCheckoutConfig(checkoutRoot)
  refusePresentInvalidIsolatedE2ePorts(checkoutRoot, config.e2e)
  const missing = refusePartialIsolatedE2ePorts(checkoutRoot, config.e2e)
  if (missing.length === 0) {
    const ports = recordedE2eApplicationPorts(config.e2e)
    publishClaim(claimRoot, config.id, ports)
    recordCheckoutPorts(checkoutRoot, config, ports)
    return ports
  }
  const held = await reserveUnclaimedApplicationPorts(
    claimedPortSet(readPublishedE2ePortClaims(claimRoot))
  )
  const ports = Object.fromEntries(
    E2E_PORT_FIELDS.map((field, index) => [field, held[index].port])
  )
  try {
    publishClaim(claimRoot, config.id, ports)
    recordCheckoutPorts(checkoutRoot, config, ports)
    return ports
  } finally {
    await closeHeldServers(held)
  }
}

export function ensureIsolatedE2ePorts(checkoutRoot, { claimRoot } = {}) {
  const root = claimRoot ?? defaultE2ePortClaimRoot()
  return withE2ePortClaimLock(root, () =>
    publishOrAllocatePorts(root, checkoutRoot)
  )
}
