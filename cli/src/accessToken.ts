import * as fs from 'node:fs'
import * as path from 'node:path'
import { configureClient, getApiConfig, UserController } from 'doughnut-api'
import { getConfigDir } from './configDir.js'
import { isFetchAbortedByCaller } from './fetchAbort.js'

export interface AccessTokenEntry {
  label: string
  token: string
}

interface AccessTokenConfig {
  tokens: AccessTokenEntry[]
  defaultLabel?: string
}

function getConfigPath(): string {
  return path.join(getConfigDir(), 'access-tokens.json')
}

function loadConfig(): AccessTokenConfig {
  try {
    const data = fs.readFileSync(getConfigPath(), 'utf-8')
    const parsed = JSON.parse(data) as AccessTokenConfig
    return { tokens: [], ...parsed }
  } catch {
    return { tokens: [] }
  }
}

function saveConfig(config: AccessTokenConfig): void {
  const p = getConfigPath()
  fs.mkdirSync(path.dirname(p), { recursive: true })
  fs.writeFileSync(p, JSON.stringify(config, null, 2), 'utf-8')
}

async function withBackendClient<T>(
  token: string,
  fn: () => Promise<T>
): Promise<T> {
  const { apiBaseUrl } = getApiConfig()
  configureClient(apiBaseUrl, token)
  try {
    return await fn()
  } catch (e) {
    if (isFetchAbortedByCaller(e)) throw e
    throw new Error(
      'Doughnut service is not available. Check DOUGHNUT_API_BASE_URL and ensure the service is running.'
    )
  }
}

export async function runWithDefaultBackendClient<T>(
  fn: () => Promise<T>
): Promise<T> {
  const config = loadConfig()
  const label = getDefaultTokenLabel()
  const entry = config.tokens.find((t) => t.label === label)
  if (!entry) {
    throw new Error(
      'No default access token. Add one first with /add-access-token.'
    )
  }
  return withBackendClient(entry.token, fn)
}

export async function addAccessToken(
  token: string,
  signal?: AbortSignal
): Promise<void> {
  const result = await withBackendClient(token, () =>
    UserController.getTokenInfo({ signal })
  )
  if (!result.data) {
    throw new Error('Token is invalid or expired.')
  }
  const config = loadConfig()
  if (config.tokens.some((t) => t.token === token)) {
    throw new Error('Token already added.')
  }
  config.tokens.push({ label: result.data.label, token })
  saveConfig(config)
}

export function listAccessTokens(): AccessTokenEntry[] {
  return loadConfig().tokens
}

export function removeAccessToken(label: string): boolean {
  const config = loadConfig()
  const index = config.tokens.findIndex((t) => t.label === label)
  if (index === -1) return false
  config.tokens.splice(index, 1)
  if (config.defaultLabel === label) {
    delete config.defaultLabel
  }
  saveConfig(config)
  return true
}

export async function removeAccessTokenCompletely(
  label: string,
  signal?: AbortSignal
): Promise<void> {
  const config = loadConfig()
  const entry = config.tokens.find((t) => t.label === label)
  if (!entry) {
    throw new Error(`Token "${label}" not found.`)
  }
  await withBackendClient(entry.token, () =>
    UserController.revokeToken({ signal })
  )
  removeAccessToken(label)
}

export function getDefaultTokenLabel(): string | undefined {
  const config = loadConfig()
  if (config.tokens.length === 0) return undefined
  if (
    config.defaultLabel &&
    config.tokens.some((t) => t.label === config.defaultLabel)
  ) {
    return config.defaultLabel
  }
  return config.tokens[0]!.label
}

export function setDefaultTokenLabel(label: string): void {
  const config = loadConfig()
  config.defaultLabel = label
  saveConfig(config)
}

export async function createAccessToken(
  label: string,
  signal?: AbortSignal
): Promise<void> {
  const config = loadConfig()
  const defaultLabel = getDefaultTokenLabel()
  const defaultEntry = config.tokens.find((t) => t.label === defaultLabel)
  if (!defaultEntry) {
    throw new Error(
      'No default access token. Add one first with /add-access-token.'
    )
  }
  const result = await withBackendClient(defaultEntry.token, () =>
    UserController.generateToken({ body: { label }, signal })
  )
  if (!result.data) {
    throw new Error('Failed to create token.')
  }
  config.tokens.push({ label: result.data.label, token: result.data.token })
  saveConfig(config)
}

export function formatTokenLines(
  tokens: AccessTokenEntry[],
  defaultLabel: string | undefined
): string[] {
  return tokens.map((t) => {
    const prefix = t.label === defaultLabel ? '★ ' : '  '
    return `${prefix}${t.label}`
  })
}

export const accessTokenCommandDocs = [
  {
    name: '/add-access-token',
    usage: '/add-access-token',
    description: 'Add a Doughnut access token',
    category: 'interactive' as const,
  },
  {
    name: '/list-access-token',
    usage: '/list-access-token',
    description: 'List stored access tokens',
    category: 'interactive' as const,
  },
  {
    name: '/remove-access-token',
    usage: '/remove-access-token',
    description: 'Remove a local access token',
    category: 'interactive' as const,
  },
  {
    name: '/remove-access-token-completely',
    usage: '/remove-access-token-completely',
    description: 'Remove token locally and from server',
    category: 'interactive' as const,
  },
  {
    name: '/create-access-token',
    usage: '/create-access-token',
    description: 'Create a new access token on the server',
    category: 'interactive' as const,
  },
]
