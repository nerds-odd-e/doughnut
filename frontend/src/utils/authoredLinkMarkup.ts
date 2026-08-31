import type { WikiLink } from "@generated/donut-backend-api"
import { hrefLooksLikePortablePath } from "@/routes/noteShowLocation"
import { parsePortablePath } from "@/utils/portablePath"

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

/** Wiki `[[inner]]` (Java INNER_LINK_PATTERN) and path Markdown `[display](/href)`. */
const INNER_LINK_PATTERN = /\[\[([^\]]+)]]/
const PATH_MARKDOWN_LINK_PATTERN = /\[([^\[\]]*)\]\((\/[^)\s]+)\)/

export type AuthoredLinkOccurrence = {
  kind: "wiki" | "pathMarkdown"
  start: number
  end: number
  /** Wiki: raw inner (no `[[ ]]`). Path Markdown: full `[display](/href)` token. */
  token: string
}

export function authoredHrefLooksLikePortablePath(href: string): boolean {
  return hrefLooksLikePortablePath(parsePortablePath(href).qualifiedNotePortion)
}

/** Path Markdown spelling: {@link WikiLink.portablePath} is the bundle-relative href. */
export function isPathMarkdownWikiLink(w: WikiLink): boolean {
  return authoredHrefLooksLikePortablePath(w.portablePath)
}

function tryParsePathMarkdownToken(
  authored: string
): { display: string; href: string } | undefined {
  const trimmed = authored.trim()
  const m = PATH_MARKDOWN_LINK_PATTERN.exec(trimmed)
  if (!m || m[0] !== trimmed) return undefined
  const href = m[2]!
  if (!authoredHrefLooksLikePortablePath(href)) return undefined
  return { display: m[1]!, href }
}

/** Target and display of an authored token: path Markdown `[display](/href)` or wiki inner. */
export function splitAuthoredToken(authored: string): {
  target: string
  display: string
} {
  const path = tryParsePathMarkdownToken(authored)
  if (path) {
    return {
      target: path.href,
      display: path.display.trim() === "" ? path.href : path.display,
    }
  }
  return splitWikiLinkInner(authored)
}

/**
 * Whole-item wiki link — `[[…]]` or path Markdown `[display](/href)`.
 * Mirrors WikiLinkMarkdown.isWellFormedWholeLinkToken.
 */
export function parseWholeWikiLinkItem(
  trimmed: string
): { inner: string; target: string; display: string } | undefined {
  if (tryParsePathMarkdownToken(trimmed)) {
    return { inner: trimmed, ...splitAuthoredToken(trimmed) }
  }
  const match = INNER_LINK_PATTERN.exec(trimmed)
  if (!match || match[0] !== trimmed) return
  const inner = match[1]!.trim()
  if (inner === "") return
  const { target, display } = splitAuthoredToken(inner)
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
  const { target: portablePath, display } = splitAuthoredToken(authored)
  return {
    authoredLink: authored,
    portablePath,
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
  const { target } = splitAuthoredToken(token)
  return noteIdByLinkKey.get(token.trim()) ?? noteIdByLinkKey.get(target.trim())
}

/** Wiki and path-Markdown link occurrences in document order (no dedupe). */
export function authoredLinkOccurrences(
  markdown: string
): AuthoredLinkOccurrence[] {
  if (markdown.length === 0) return []
  const hits: AuthoredLinkOccurrence[] = []
  for (const m of markdown.matchAll(new RegExp(INNER_LINK_PATTERN, "g"))) {
    const inner = m[1]!
    if (inner.trim() === "") continue
    const start = m.index
    hits.push({ kind: "wiki", start, end: start + m[0].length, token: inner })
  }
  for (const m of markdown.matchAll(
    new RegExp(PATH_MARKDOWN_LINK_PATTERN, "g")
  )) {
    const start = m.index
    if (start > 0 && markdown[start - 1] === "!") continue
    const href = m[2]!
    if (!authoredHrefLooksLikePortablePath(href)) continue
    hits.push({
      kind: "pathMarkdown",
      start,
      end: start + m[0].length,
      token: m[0],
    })
  }
  hits.sort((a, b) => a.start - b.start)
  return hits
}
