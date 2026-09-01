import type { WikiLink } from "@generated/donut-backend-api"

/** Splits inner wiki text on the first `|`; empty right-hand side is treated as no pipe. */
export function splitWikiLinkInner(rawBetweenBrackets: string): {
  target: string
  display: string
} {
  const i = rawBetweenBrackets.indexOf("|")
  if (i === -1) {
    return { target: rawBetweenBrackets, display: rawBetweenBrackets }
  }
  const target = rawBetweenBrackets.slice(0, i)
  const display = rawBetweenBrackets.slice(i + 1)
  if (display.trim().length === 0) {
    return { target, display: target }
  }
  return { target, display }
}

/** Wiki `[[inner]]` (Java INNER_LINK_PATTERN). */
const INNER_LINK_PATTERN = /\[\[([^\]]+)]]/

export type AuthoredLinkOccurrence = {
  start: number
  end: number
  /** Raw inner (no `[[ ]]`). */
  token: string
}

/**
 * Whole-item wiki link — `[[…]]`.
 * Mirrors WikiLinkMarkdown.isWellFormedWholeLinkToken.
 */
export function parseWholeWikiLinkItem(
  trimmed: string
): { inner: string; target: string; display: string } | undefined {
  const match = INNER_LINK_PATTERN.exec(trimmed)
  if (!match || match[0] !== trimmed) return
  const inner = match[1]!.trim()
  if (inner === "") return
  const { target, display } = splitWikiLinkInner(inner)
  if (target.trim() === "") return
  return { inner, target, display }
}

export function isWellFormedWholeWikiLinkItem(trimmed: string): boolean {
  return parseWholeWikiLinkItem(trimmed) !== undefined
}

/** Builds API-shaped {@link WikiLink} for tests and local fixtures from an authored token + note id. */
export function wikiLinkFromAuthoredToken(
  authored: string,
  noteId: number
): WikiLink {
  const { target, display } = splitWikiLinkInner(authored)
  return {
    authoredLink: authored,
    target,
    displayText: display,
    resolution: "RESOLVED",
    destinationNoteId: noteId,
  }
}

/** Note id for an authored token: full token key, else split target. */
export function noteIdForAuthoredToken(
  token: string,
  noteIdByLinkKey: Map<string, number>
): number | undefined {
  const { target } = splitWikiLinkInner(token)
  return noteIdByLinkKey.get(token.trim()) ?? noteIdByLinkKey.get(target.trim())
}

/** Wiki link occurrences in document order (no dedupe). */
export function authoredLinkOccurrences(
  markdown: string
): AuthoredLinkOccurrence[] {
  if (markdown.length === 0) return []
  const hits: AuthoredLinkOccurrence[] = []
  for (const m of markdown.matchAll(new RegExp(INNER_LINK_PATTERN, "g"))) {
    const inner = m[1]!
    if (inner.trim() === "") continue
    const start = m.index
    hits.push({ start, end: start + m[0].length, token: inner })
  }
  return hits
}
