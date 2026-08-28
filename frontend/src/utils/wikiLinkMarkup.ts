import type { WikiTitle } from "@generated/donut-backend-api"
import {
  hrefLooksLikeConceptNotePath,
  noteShowHref,
} from "@/routes/noteShowLocation"
import {
  DEAD_WIKI_LINK_CLASS,
  DONUT_WIKI_LINK_CLASS,
  PENDING_WIKI_LINK_CLASS,
} from "@/utils/wikiLinkDomMarkers"

export {
  splitWikiLinkInner,
  wikiTitleFromAuthoredToken,
} from "@/utils/authoredLinkMarkup"

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
  return hrefLooksLikeConceptNotePath(w.targetToken)
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

/** Wiki-style `<a>`: live/dead class, `data-wiki-title`, optional display and note id. */
export function wikiLinkAnchorHtml(attrs: {
  href: string
  className: string
  target: string
  display: string
  noteId?: number
}): string {
  const attrHref = escapeHtmlAttributeValue(attrs.href)
  const attrTarget = escapeHtmlAttributeValue(attrs.target)
  const displayAttr =
    attrs.display !== attrs.target
      ? ` data-wiki-display="${escapeHtmlAttributeValue(attrs.display)}"`
      : ""
  const noteIdAttr =
    attrs.noteId === undefined ? "" : ` data-note-id="${attrs.noteId}"`
  return `<a href="${attrHref}" class="${attrs.className}" data-wiki-title="${attrTarget}"${displayAttr}${noteIdAttr}>${escapeHtmlForWikiLinkDisplay(attrs.display)}</a>`
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
  if (anchor.classList.contains(PENDING_WIKI_LINK_CLASS)) {
    return
  }
  const noteId = anchor.getAttribute("data-note-id")
  if (anchor.classList.contains(DONUT_WIKI_LINK_CLASS) && noteId) {
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
