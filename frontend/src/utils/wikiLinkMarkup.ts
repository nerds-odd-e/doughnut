import type { WikiLink } from "@generated/donut-backend-api"
import type { RouteLocationRaw } from "vue-router"
import {
  authoredHrefLooksLikePortablePath,
  splitWikiLinkInner,
  wikiLinkFromAuthoredToken,
} from "@/utils/authoredLinkMarkup"
import {
  DEAD_WIKI_LINK_CLASS,
  DONUT_WIKI_LINK_CLASS,
  PENDING_WIKI_LINK_CLASS,
  WIKI_LINK_DISPLAY_TEXT_ATTR,
  WIKI_LINK_PORTABLE_PATH_ATTR,
} from "@/utils/wikiLinkDomMarkers"
import { locationForResolvedWikiTarget } from "@/utils/wikiLinkResolvedLocation"

export { splitWikiLinkInner, wikiLinkFromAuthoredToken }

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

/** Wiki-style `<a>`: live/dead class, `data-portable-path`, optional display, note id, and already-escaped inner HTML. */
export function wikiLinkAnchorHtml(attrs: {
  href: string
  className: string
  target: string
  display: string
  noteId?: number
  innerHtml?: string
}): string {
  const attrHref = escapeHtmlAttributeValue(attrs.href)
  const attrTarget = escapeHtmlAttributeValue(attrs.target)
  const displayAttr =
    attrs.display !== attrs.target
      ? ` ${WIKI_LINK_DISPLAY_TEXT_ATTR}="${escapeHtmlAttributeValue(attrs.display)}"`
      : ""
  const noteIdAttr =
    attrs.noteId === undefined ? "" : ` data-note-id="${attrs.noteId}"`
  const body = attrs.innerHtml ?? escapeHtmlForWikiLinkDisplay(attrs.display)
  return `<a href="${attrHref}" class="${attrs.className}" ${WIKI_LINK_PORTABLE_PATH_ATTR}="${attrTarget}"${displayAttr}${noteIdAttr}>${body}</a>`
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

/** True when the payload names a single destination note. */
export function isResolvedWikiLink(
  w: WikiLink
): w is WikiLink & { destinationNoteId: number } {
  return w.resolution === "RESOLVED" && w.destinationNoteId !== undefined
}

/** Lookup keys: trimmed Portable path and full authored link from the note realm. */
export function wikiLinkNoteIdLookup(
  wikiLinks: readonly WikiLink[]
): Map<string, number> {
  const map = new Map<string, number>()
  for (const w of wikiLinks) {
    if (!isResolvedWikiLink(w)) continue
    map.set(w.portablePath.trim(), w.destinationNoteId)
    map.set(w.authoredLink.trim(), w.destinationNoteId)
  }
  return map
}

/** Dead wiki link click payload containing the target token and visible display text. */
export type DeadWikiLinkPayload = { portablePath: string; displayText: string }

function pathMarkdownToken(displayText: string, href: string): string {
  return `[${displayText}](${href})`
}

/** Stored token for the clicked dead wiki link (wiki `[[…]]` or path `[label](href)`), for replace. */
export function markdownWikiTokenFromDeadWikiLinkPayload(
  p: DeadWikiLinkPayload
): string {
  const { portablePath, displayText } = p
  if (authoredHrefLooksLikePortablePath(portablePath)) {
    return pathMarkdownToken(displayText, portablePath)
  }
  if (portablePath === displayText) return `[[${portablePath}]]`
  return `[[${portablePath}|${displayText}]]`
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
    navigateInApp: (to: RouteLocationRaw) => void
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
    handlers.navigateInApp(
      locationForResolvedWikiTarget(
        Number(noteId),
        anchor.getAttribute(WIKI_LINK_PORTABLE_PATH_ATTR) ?? ""
      )
    )
    return
  }
  const href = anchor.getAttribute("href")
  if (!href) return
  if (/^https?:\/\//i.test(href) || href.startsWith("//")) {
    window.open(href, "_blank", "noopener,noreferrer")
    return
  }
  if (href === "#" || authoredHrefLooksLikePortablePath(href)) return
  handlers.navigateInApp(href)
}

/** Extracts target token and display text from a dead-wiki-link anchor element. */
export function deadWikiLinkPayloadFromAnchor(
  anchor: HTMLElement
): DeadWikiLinkPayload {
  const raw = anchor.textContent?.trim() ?? ""
  let portablePath: string
  const fromAttr = anchor.getAttribute(WIKI_LINK_PORTABLE_PATH_ATTR)
  if (fromAttr !== null && fromAttr !== "") {
    portablePath = fromAttr
  } else {
    const closed = /^\[\[([^\[\]\r\n]*)\]\]$/.exec(raw)
    if (closed?.[1] !== undefined) {
      portablePath = closed[1].trim()
    } else {
      const open = /^\[\[([^\[\]\r\n]*)$/.exec(raw)
      portablePath = open?.[1]?.trim() ?? raw
    }
  }

  const displayAttr = anchor.getAttribute(WIKI_LINK_DISPLAY_TEXT_ATTR)
  if (displayAttr !== null && displayAttr !== "") {
    return { portablePath, displayText: displayAttr }
  }
  return { portablePath, displayText: portablePath }
}

function pathHrefFromWikiAnchor(anchor: HTMLAnchorElement): string | null {
  const fromAttr = anchor.getAttribute(WIKI_LINK_PORTABLE_PATH_ATTR)
  if (
    fromAttr !== null &&
    fromAttr !== "" &&
    authoredHrefLooksLikePortablePath(fromAttr)
  ) {
    return fromAttr
  }
  const href = anchor.getAttribute("href")
  if (href && authoredHrefLooksLikePortablePath(href)) {
    return href
  }
  return null
}

/** Markdown token for a wiki anchor (dead or live) from DOM; prefers `data-portable-path` / bracketed display. */
export function wikiAnchorToMarkdownToken(anchor: HTMLAnchorElement): string {
  const pathHref = pathHrefFromWikiAnchor(anchor)
  if (pathHref !== null) {
    const display =
      anchor.getAttribute(WIKI_LINK_DISPLAY_TEXT_ATTR) ||
      anchor.textContent?.trim() ||
      ""
    return pathMarkdownToken(display, pathHref)
  }

  const raw = anchor.textContent?.trim() ?? ""
  const target = anchor.getAttribute(WIKI_LINK_PORTABLE_PATH_ATTR)
  if (target === null || target === "") {
    const bracketed = /^\[\[([\s\S]*)\]\]$/.exec(raw)
    if (bracketed?.[1] !== undefined) {
      return `[[${bracketed[1]}]]`
    }
    return `[[${raw}]]`
  }

  const fromDisplayAttr = anchor.getAttribute(WIKI_LINK_DISPLAY_TEXT_ATTR)
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
