import * as fs from 'node:fs'
import * as path from 'node:path'
import type { GeneratedTokenDto, TokenConfigDto } from 'doughnut-api'
import { getConfigDir } from '../configDir.js'

export type AccessTokenEntry = Pick<GeneratedTokenDto, 'label' | 'token'>
type AccessTokenLabel = TokenConfigDto['label']

export interface AccessTokenConfig {
  tokens: AccessTokenEntry[]
  defaultLabel?: AccessTokenLabel
}

export function defaultAccessTokenLabel(
  config: AccessTokenConfig
): AccessTokenLabel | undefined {
  if (config.tokens.length === 0) return undefined
  if (
    config.defaultLabel &&
    config.tokens.some((t) => t.label === config.defaultLabel)
  ) {
    return config.defaultLabel
  }
  return config.tokens[0]!.label
}

function getConfigPath(): string {
  return path.join(getConfigDir(), 'access-tokens.json')
}

export function loadAccessTokenConfig(): AccessTokenConfig {
  try {
    const data = fs.readFileSync(getConfigPath(), 'utf-8')
    const parsed = JSON.parse(data) as Partial<AccessTokenConfig>
    return { tokens: parsed.tokens ?? [], defaultLabel: parsed.defaultLabel }
  } catch {
    return { tokens: [] }
  }
}

export function saveAccessTokenConfig(config: AccessTokenConfig): void {
  const p = getConfigPath()
  fs.mkdirSync(path.dirname(p), { recursive: true })
  fs.writeFileSync(p, JSON.stringify(config, null, 2), 'utf-8')
}

export function appendStoredAccessToken(entry: AccessTokenEntry): void {
  const config = loadAccessTokenConfig()
  if (config.tokens.some((t) => t.token === entry.token)) {
    throw new Error('Token already added.')
  }
  config.tokens.push(entry)
  saveAccessTokenConfig(config)
}
