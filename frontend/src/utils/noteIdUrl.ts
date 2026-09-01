/**
 * Canonical compact Donut note-show URL recognition (ADR 0005). Root-relative `/n<ID>` and absolute
 * HTTP(S) URLs on a configured canonical origin — not retired `/n/<ID>` redirects, property paths,
 * or query/hash variants.
 */

const ROOT_RELATIVE_NOTE_HREF = /^\/n(\d+)$/

/** Production default; tests may pass a different origin to recognition helpers. */
export const PRODUCTION_CANONICAL_DONUT_ORIGIN = "https://doughnut.odd-e.com"

/** Note id when href is exactly the canonical compact path `/n<digits>`. */
export function noteIdFromRootRelativeHref(href: string): number | undefined {
  const m = ROOT_RELATIVE_NOTE_HREF.exec(href)
  if (!m) return undefined
  return Number(m[1])
}

function normalizeOrigin(origin: string): string {
  const trimmed = origin.trim().replace(/\/$/, "")
  const u = new URL(trimmed)
  const scheme = u.protocol.replace(/:$/, "").toLowerCase()
  const host = u.hostname.toLowerCase()
  if (u.port) return `${scheme}://${host}:${u.port}`
  return `${scheme}://${host}`
}

function noteIdFromAbsoluteCanonicalHref(
  href: string,
  canonicalOrigin: string
): number | undefined {
  let u: URL
  try {
    u = new URL(href)
  } catch {
    return undefined
  }
  if (u.protocol !== "http:" && u.protocol !== "https:") return undefined
  if (u.search !== "" || u.hash !== "") return undefined
  if (normalizeOrigin(u.origin) !== normalizeOrigin(canonicalOrigin))
    return undefined
  return noteIdFromRootRelativeHref(u.pathname)
}

/**
 * Note id when href is a root-relative canonical path or an exact HTTP(S) URL on
 * `canonicalOrigin` with that path (no query or fragment).
 */
export function noteIdFromHref(
  href: string,
  canonicalOrigin: string = PRODUCTION_CANONICAL_DONUT_ORIGIN
): number | undefined {
  return (
    noteIdFromRootRelativeHref(href) ??
    noteIdFromAbsoluteCanonicalHref(href, canonicalOrigin)
  )
}

/** Markdown `[display](href)`; skips the second `[` of wiki `[[…]]`. */
export const MARKDOWN_LINK = /(?<!\[)\[([^\]]*)]\(([^)]+)\)/g
