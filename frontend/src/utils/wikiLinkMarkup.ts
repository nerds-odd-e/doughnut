import type { WikiTitle } from "@generated/doughnut-backend-api"
import {
  hrefLooksLikeConceptNotePath,
  noteShowHref,
} from "@/routes/noteShowLocation"
import {
  DEAD_WIKI_LINK_CLASS,
  DOUGHNUT_WIKI_LINK_CLASS,
} from "@/utils/wikiLinkDomMarkers"

/** Builds API-shaped {@link WikiTitle} for tests and local fixtures from markdown inner + note id. */
export function wikiTitleFromInnerAndNoteId(
  inner: string,
  noteId: number
): WikiTitle {
  const { target, display } = splitWikiLinkInner(inner)
  return { linkText: inner, targetToken: target, displayText: display, noteId }
}

/** Normalized target, display label, and full inner for a wiki title from the note realm. */
export function wikiTitleParts(w: WikiTitle): {
  target: string
  display: string
  inner: string
} {
  return { target: w.targetToken, display: w.displayText, inner: w.linkText }
}

/** Path Markdown spelling: {@link WikiTitle.targetToken} is the bundle-relative href. */
export function isPathMarkdownWikiTitle(w: WikiTitle): boolean {
  return w.targetToken.startsWith("/")
}

export function escapeHtmlForWikiLinkDisplay(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
}

export function escapeHtmlAttributeValue(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/"/g, "&quot;").replace(/</g, "&lt;")
}

/** `[[` / `]]` shown literally; title text escaped (same visible shape as plain wiki syntax). */
export function wikiLinkBracketedInnerHtml(plainTitleInner: string): string {
  return `<span class="wiki-bracket">[[</span>${escapeHtmlForWikiLinkDisplay(plainTitleInner)}<span class="wiki-bracket">]]</span>`
}

/** Valid wiki segment: non-empty after trim, no brackets or newlines inside. */
export function isValidWikiLinkInner(rawBetweenBrackets: string): boolean {
  return (
    rawBetweenBrackets.trim().length > 0 &&
    !/[\[\]\r\n]/.test(rawBetweenBrackets)
  )
}

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

/** Lookup keys: trimmed wiki target token and full `linkText` from the note realm. */
export function wikiTitleNoteIdLookup(
  wikiTitles: readonly WikiTitle[]
): Map<string, number> {
  const map = new Map<string, number>()
  for (const w of wikiTitles) {
    const { target } = wikiTitleParts(w)
    map.set(target.trim(), w.noteId)
    map.set(w.linkText.trim(), w.noteId)
  }
  return map
}

/** Dead wiki link click payload containing the target token and visible display text. */
export type DeadWikiLinkPayload = { targetToken: string; displayText: string }

function pathMarkdownToken(displayText: string, href: string): string {
  return `[${displayText}](${href})`
}

/** Stored token for the clicked dead wiki link (wiki `[[…]]` or path `[label](href)`), for replace. */
export function markdownWikiTokenFromDeadWikiLinkPayload(
  p: DeadWikiLinkPayload
): string {
  const { targetToken, displayText } = p
  if (hrefLooksLikeConceptNotePath(targetToken)) {
    return pathMarkdownToken(displayText, targetToken)
  }
  if (targetToken === displayText) return `[[${targetToken}]]`
  return `[[${targetToken}|${displayText}]]`
}

/** Path-Markdown token pointing at a note, keeping the authored `.md` / no-`.md` suffix. */
export function pathMarkdownTokenForNote(args: {
  displayText: string
  folderNames: readonly string[]
  title: string
  authoredHref: string
}): string {
  const folders = args.folderNames.filter((name) => name.length > 0)
  const path =
    folders.length > 0 ? `${folders.join("/")}/${args.title}` : args.title
  const suffix = args.authoredHref.toLowerCase().endsWith(".md") ? ".md" : ""
  return pathMarkdownToken(args.displayText, `/${path}${suffix}`)
}

/** Handles click on a rich-content anchor: dead wiki links, external URLs, in-app routes. */
export function handleRichContentAnchorClick(
  anchor: HTMLAnchorElement,
  handlers: {
    onDeadWikiLink: (payload: DeadWikiLinkPayload) => void
    navigateInApp: (href: string) => void
  },
  options: { deadWikiLinksEnabled: boolean }
): void {
  if (
    options.deadWikiLinksEnabled &&
    anchor.classList.contains(DEAD_WIKI_LINK_CLASS)
  ) {
    handlers.onDeadWikiLink(deadWikiLinkPayloadFromAnchor(anchor))
    return
  }
  const noteId = anchor.getAttribute("data-note-id")
  if (anchor.classList.contains(DOUGHNUT_WIKI_LINK_CLASS) && noteId) {
    handlers.navigateInApp(noteShowHref(Number(noteId)))
    return
  }
  const href = anchor.getAttribute("href")
  if (!href) return
  if (/^https?:\/\//i.test(href) || href.startsWith("//")) {
    window.open(href, "_blank", "noopener,noreferrer")
    return
  }
  handlers.navigateInApp(href)
}

/** Extracts target token and display text from a dead-wiki-link anchor element. */
export function deadWikiLinkPayloadFromAnchor(
  anchor: HTMLElement
): DeadWikiLinkPayload {
  const raw = anchor.textContent?.trim() ?? ""
  let targetToken: string
  const fromAttr = anchor.getAttribute("data-wiki-title")
  if (fromAttr !== null && fromAttr !== "") {
    targetToken = fromAttr
  } else {
    const closed = /^\[\[([^\[\]\r\n]*)\]\]$/.exec(raw)
    if (closed?.[1] !== undefined) {
      targetToken = closed[1].trim()
    } else {
      const open = /^\[\[([^\[\]\r\n]*)$/.exec(raw)
      targetToken = open?.[1]?.trim() ?? raw
    }
  }

  const displayAttr = anchor.getAttribute("data-wiki-display")
  if (displayAttr !== null && displayAttr !== "") {
    return { targetToken, displayText: displayAttr }
  }
  return { targetToken, displayText: targetToken }
}

function pathHrefFromWikiAnchor(anchor: HTMLAnchorElement): string | null {
  const fromAttr = anchor.getAttribute("data-wiki-title")
  if (
    fromAttr !== null &&
    fromAttr !== "" &&
    hrefLooksLikeConceptNotePath(fromAttr)
  ) {
    return fromAttr
  }
  const href = anchor.getAttribute("href")
  if (href && hrefLooksLikeConceptNotePath(href)) {
    return href
  }
  return null
}

/** Markdown token for a wiki anchor (dead or live) from DOM; prefers `data-wiki-title` / bracketed display. */
export function wikiAnchorToMarkdownToken(anchor: HTMLAnchorElement): string {
  const pathHref = pathHrefFromWikiAnchor(anchor)
  if (pathHref !== null) {
    const display =
      anchor.getAttribute("data-wiki-display") ||
      anchor.textContent?.trim() ||
      ""
    return pathMarkdownToken(display, pathHref)
  }

  const raw = anchor.textContent?.trim() ?? ""
  const target = anchor.getAttribute("data-wiki-title")
  if (target === null || target === "") {
    const bracketed = /^\[\[([\s\S]*)\]\]$/.exec(raw)
    if (bracketed?.[1] !== undefined) {
      return `[[${bracketed[1]}]]`
    }
    return `[[${raw}]]`
  }

  const fromDisplayAttr = anchor.getAttribute("data-wiki-display")
  const innerM = /^\[\[([\s\S]*)\]\]$/.exec(raw)

  if (fromDisplayAttr !== null && fromDisplayAttr !== "") {
    const displayPart = fromDisplayAttr
    if (displayPart === target) {
      return `[[${target}]]`
    }
    return `[[${target}|${displayPart}]]`
  }

  if (innerM !== null) {
    const visibleInner = innerM[1]!
    if (visibleInner === target) {
      return `[[${target}]]`
    }
    return `[[${target}|${visibleInner}]]`
  }

  if (raw.startsWith("[[") && !raw.endsWith("]]")) {
    return raw
  }

  if (raw === target) {
    return `[[${target}]]`
  }
  return `[[${target}|${raw}]]`
}
