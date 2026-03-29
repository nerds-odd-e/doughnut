import * as fs from 'node:fs'
import * as path from 'node:path'
import type { GeneratedTokenDto, TokenConfigDto } from 'doughnut-api'
import { getConfigDir } from '../configDir.js'

export type AccessTokenEntry = Pick<GeneratedTokenDto, 'label' | 'token'>
type AccessTokenLabel = TokenConfigDto['label']

interface AccessTokenConfig {
  tokens: AccessTokenEntry[]
  defaultLabel?: AccessTokenLabel
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

export function getDefaultTokenLabel(): AccessTokenLabel | undefined {
  const config = loadAccessTokenConfig()
  if (config.tokens.length === 0) return undefined
  if (
    config.defaultLabel &&
    config.tokens.some((t) => t.label === config.defaultLabel)
  ) {
    return config.defaultLabel
  }
  return config.tokens[0]!.label
}

export function setDefaultTokenLabel(label: AccessTokenLabel): void {
  const config = loadAccessTokenConfig()
  if (!config.tokens.some((t) => t.label === label)) {
    throw new Error(`No access token stored with label "${label}".`)
  }
  config.defaultLabel = label
  saveAccessTokenConfig(config)
}

export function getStoredAccessTokenLabels(): string[] {
  return loadAccessTokenConfig().tokens.map((t) => t.label)
}

export function appendStoredAccessToken(entry: AccessTokenEntry): void {
  const config = loadAccessTokenConfig()
  if (config.tokens.some((t) => t.token === entry.token)) {
    throw new Error('Token already added.')
  }
  config.tokens.push(entry)
  saveAccessTokenConfig(config)
}

export function removeAccessTokenLocal(label: string): void {
  const config = loadAccessTokenConfig()
  const idx = config.tokens.findIndex((t) => t.label === label)
  if (idx < 0) {
    throw new Error(`No access token stored with label "${label}".`)
  }
  config.tokens.splice(idx, 1)
  if (config.tokens.length === 0) {
    delete config.defaultLabel
  } else if (config.defaultLabel === label) {
    config.defaultLabel = config.tokens[0]!.label
  }
  saveAccessTokenConfig(config)
}

export function getStoredAccessTokenEntryByLabel(
  label: string
): AccessTokenEntry | undefined {
  return loadAccessTokenConfig().tokens.find((t) => t.label === label)
}
