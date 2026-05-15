export const SEARCH_KEY_HISTORY_COOKIE_NAME = "doughnut.searchKeyHistory"

const MAX_KEYS = 100
/** Stored substring length so long queries still fit in a single cookie with ~4KB total limit. */
const MAX_CHARS_PER_KEY = 512
const MAX_AGE_SECONDS = 60 * 60 * 24 * 400
const MAX_COOKIE_PAYLOAD_CHARS = 3800

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

export function readSearchKeyHistory(): string[] {
  if (typeof document === "undefined") return []
  const encoded = parseCookieValue(
    document.cookie,
    SEARCH_KEY_HISTORY_COOKIE_NAME
  )
  if (encoded == null || encoded === "") return []
  try {
    const parsed: unknown = JSON.parse(decodeURIComponent(encoded))
    if (!Array.isArray(parsed)) return []
    return parsed.filter((x): x is string => typeof x === "string")
  } catch {
    return []
  }
}

function cookiePayloadWithinLimit(keys: string[]): string {
  let list = [...keys]
  let payload = encodeURIComponent(JSON.stringify(list))
  while (payload.length > MAX_COOKIE_PAYLOAD_CHARS && list.length > 1) {
    list = list.slice(0, -1)
    payload = encodeURIComponent(JSON.stringify(list))
  }
  return payload
}

function writeSearchKeyHistory(keys: string[]): void {
  if (typeof document === "undefined") return
  const payload = cookiePayloadWithinLimit(keys)
  document.cookie = `${SEARCH_KEY_HISTORY_COOKIE_NAME}=${payload}; Path=/; Max-Age=${MAX_AGE_SECONDS}; SameSite=Lax`
}

export function appendSearchKeyToHistory(rawKey: string): void {
  const key = normalizeKey(rawKey)
  if (key === "") return
  const existing = readSearchKeyHistory()
  const withoutDup = existing.filter((k) => k !== key)
  const next = [key, ...withoutDup].slice(0, MAX_KEYS)
  writeSearchKeyHistory(next)
}

/** Test helper: remove the history cookie. */
export function clearSearchKeyHistoryCookie(): void {
  if (typeof document === "undefined") return
  document.cookie = `${SEARCH_KEY_HISTORY_COOKIE_NAME}=; Path=/; Max-Age=0; SameSite=Lax`
}
