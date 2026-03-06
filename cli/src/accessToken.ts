import * as fs from 'node:fs'
import * as os from 'node:os'
import * as path from 'node:path'
import { client } from '@generated/backend/client.gen'
import { UserController } from '@generated/backend/sdk.gen'

export interface AccessTokenEntry {
  label: string
  token: string
}

interface AccessTokenConfig {
  tokens: AccessTokenEntry[]
}

function getConfigDir(): string {
  const dir = process.env.DOUGHNUT_CONFIG_DIR
  if (dir) return dir
  return path.join(os.homedir(), '.config', 'doughnut')
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

export async function addAccessToken(token: string): Promise<void> {
  const baseUrl = process.env.DOUGHNUT_API_BASE_URL || 'http://localhost:9081'
  client.setConfig({
    baseUrl,
    headers: { Authorization: `Bearer ${token}` },
  })
  let data: { label: string } | undefined
  try {
    const result = await UserController.getTokenInfo()
    data = result.data
  } catch {
    throw new Error(
      'Doughnut service is not available. Check DOUGHNUT_API_BASE_URL and ensure the service is running.'
    )
  }
  if (!data) {
    throw new Error('Token is invalid or expired.')
  }
  const config = loadConfig()
  config.tokens.push({ label: data.label, token })
  saveConfig(config)
  console.log('Token added')
}

export function listAccessTokens(): AccessTokenEntry[] {
  return loadConfig().tokens
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
]
