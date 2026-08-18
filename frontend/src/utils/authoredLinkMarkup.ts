import { hrefLooksLikeConceptNotePath } from "@/routes/noteShowLocation"

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

function tryParsePathMarkdownToken(
  authored: string
): { display: string; href: string } | undefined {
  const trimmed = authored.trim()
  const m = PATH_MARKDOWN_LINK_PATTERN.exec(trimmed)
  if (!m || m[0] !== trimmed) return undefined
  const href = m[2]!
  if (!hrefLooksLikeConceptNotePath(href)) return undefined
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
    if (!hrefLooksLikeConceptNotePath(href)) continue
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
