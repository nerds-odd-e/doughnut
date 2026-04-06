import * as fs from 'node:fs'
import * as path from 'node:path'
import { getConfigDir } from '../configDir.js'

export interface StoredAccessToken {
  token: string
  label?: string
}

type LegacyTokenEntry = { label?: string; token?: string }
type LegacyFile = {
  tokens?: LegacyTokenEntry[]
  defaultLabel?: string
  token?: string
  label?: string
}

function pickLegacyToken(parsed: LegacyFile): StoredAccessToken | undefined {
  if (typeof parsed.token === 'string' && parsed.token !== '') {
    return {
      token: parsed.token,
      label: typeof parsed.label === 'string' ? parsed.label : undefined,
    }
  }
  const tokens = parsed.tokens
  if (!Array.isArray(tokens) || tokens.length === 0) return undefined
  const asEntry = (e: unknown): StoredAccessToken | undefined => {
    if (!e || typeof e !== 'object') return undefined
    const o = e as LegacyTokenEntry
    if (typeof o.token !== 'string' || o.token === '') return undefined
    return {
      token: o.token,
      label: typeof o.label === 'string' ? o.label : undefined,
    }
  }
  const dl = parsed.defaultLabel
  if (typeof dl === 'string') {
    for (const t of tokens) {
      if (t && typeof t === 'object' && (t as LegacyTokenEntry).label === dl) {
        const r = asEntry(t)
        if (r) return r
      }
    }
  }
  for (const t of tokens) {
    const r = asEntry(t)
    if (r) return r
  }
  return undefined
}

function getConfigPath(): string {
  return path.join(getConfigDir(), 'access-tokens.json')
}

/** Loads the single stored API token, migrating legacy `tokens` / `defaultLabel` files on read. */
export function loadStoredAccessToken(): StoredAccessToken | undefined {
  try {
    const data = fs.readFileSync(getConfigPath(), 'utf-8')
    const parsed = JSON.parse(data) as LegacyFile
    return pickLegacyToken(parsed)
  } catch {
    return undefined
  }
}

export function saveStoredAccessToken(entry: StoredAccessToken): void {
  const p = getConfigPath()
  fs.mkdirSync(path.dirname(p), { recursive: true })
  const out: Record<string, string> = { token: entry.token }
  if (entry.label !== undefined && entry.label !== '') {
    out.label = entry.label
  }
  fs.writeFileSync(p, JSON.stringify(out, null, 2), 'utf-8')
}
