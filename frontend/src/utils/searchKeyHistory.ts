export const SEARCH_KEY_HISTORY_KEY = "donut.searchKeyHistory"

const MAX_KEYS = 100
const MAX_CHARS_PER_KEY = 512

function normalizeKey(raw: string): string {
  const t = raw.trim()
  if (t.length <= MAX_CHARS_PER_KEY) return t
  return t.slice(0, MAX_CHARS_PER_KEY)
}

function parseCookieValue(
  rawDocumentCookie: string,
  name: string
): string | null {
  const parts = rawDocumentCookie.split("; ")
  for (const part of parts) {
    if (part.startsWith(`${name}=`)) {
      return part.slice(name.length + 1)
    }
  }
  return null
}

function normalizeHistory(keys: unknown[]): string[] {
  return [
    ...new Set(
      keys
        .filter((key): key is string => typeof key === "string")
        .map(normalizeKey)
        .filter(Boolean)
    ),
  ].slice(0, MAX_KEYS)
}

function parseHistory(raw: string | null): string[] | null {
  if (raw === null) return null
  let parsed: unknown
  try {
    parsed = JSON.parse(raw)
  } catch (error) {
    if (error instanceof SyntaxError) return null
    throw error
  }
  return Array.isArray(parsed) ? normalizeHistory(parsed) : null
}

function isStorageUnavailable(error: unknown): boolean {
  return (
    error instanceof DOMException &&
    (error.name === "SecurityError" || error.name === "QuotaExceededError")
  )
}

function readLocalHistory(): string[] | null {
  let raw: string | null
  try {
    raw = localStorage.getItem(SEARCH_KEY_HISTORY_KEY)
  } catch (error) {
    if (isStorageUnavailable(error)) return null
    throw error
  }
  return parseHistory(raw)
}

function readLegacyHistory(): string[] | null {
  const encoded = parseCookieValue(document.cookie, SEARCH_KEY_HISTORY_KEY)
  if (!encoded) return null
  let decoded: string
  try {
    decoded = decodeURIComponent(encoded)
  } catch (error) {
    if (error instanceof URIError) return null
    throw error
  }
  return parseHistory(decoded)
}

export function readSearchKeyHistory(): string[] {
  if (typeof document === "undefined") return []
  return readLocalHistory() ?? readLegacyHistory() ?? []
}

function expireLegacyHistory(): void {
  document.cookie = `${SEARCH_KEY_HISTORY_KEY}=; Path=/; Max-Age=0; SameSite=Lax`
}

function writeSearchKeyHistory(keys: string[]): void {
  if (typeof document === "undefined") return
  const payload = JSON.stringify(keys)
  try {
    localStorage.setItem(SEARCH_KEY_HISTORY_KEY, payload)
  } catch (error) {
    if (isStorageUnavailable(error)) return
    throw error
  }
  expireLegacyHistory()
}

export function migrateSearchKeyHistory(): void {
  if (typeof document === "undefined") return
  if (readLocalHistory() !== null) {
    expireLegacyHistory()
    return
  }
  const legacyHistory = readLegacyHistory()
  if (legacyHistory !== null) writeSearchKeyHistory(legacyHistory)
}

export function appendSearchKeyToHistory(rawKey: string): void {
  const key = normalizeKey(rawKey)
  if (key === "") return
  const existing = readSearchKeyHistory()
  const withoutDup = existing.filter((k) => k !== key)
  const next = [key, ...withoutDup].slice(0, MAX_KEYS)
  writeSearchKeyHistory(next)
}
