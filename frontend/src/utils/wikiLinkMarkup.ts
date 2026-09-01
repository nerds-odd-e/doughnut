import type { WikiLink } from "@generated/donut-backend-api"
import {
  splitWikiLinkInner,
  wikiLinkFromAuthoredToken,
} from "@/utils/authoredLinkMarkup"
import {
  WIKI_LINK_DISPLAY_TEXT_ATTR,
  WIKI_LINK_PORTABLE_PATH_ATTR,
  WIKI_LINK_RESOLUTION_ATTR,
} from "@/utils/wikiLinkDomMarkers"
import {
  deadWikiLinkPayloadFromAnchor,
  handleRichContentAnchorClick,
  type DeadWikiLinkPayload,
} from "@/utils/wikiLinkClick"

export { splitWikiLinkInner, wikiLinkFromAuthoredToken }
export {
  deadWikiLinkPayloadFromAnchor,
  handleRichContentAnchorClick,
  type DeadWikiLinkPayload,
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

/** Wiki-style `<a>`: live/dead class, `data-portable-path`, optional display, note id, and already-escaped inner HTML. */
export function wikiLinkAnchorHtml(attrs: {
  href: string
  className: string
  portablePath: string
  display: string
  noteId?: number
  innerHtml?: string
  resolution?: "AMBIGUOUS"
}): string {
  const attrHref = escapeHtmlAttributeValue(attrs.href)
  const attrPortablePath = escapeHtmlAttributeValue(attrs.portablePath)
  const displayAttr =
    attrs.display !== attrs.portablePath
      ? ` ${WIKI_LINK_DISPLAY_TEXT_ATTR}="${escapeHtmlAttributeValue(attrs.display)}"`
      : ""
  const noteIdAttr =
    attrs.noteId === undefined ? "" : ` data-note-id="${attrs.noteId}"`
  const resolutionAttr =
    attrs.resolution === undefined
      ? ""
      : ` ${WIKI_LINK_RESOLUTION_ATTR}="${attrs.resolution}"`
  const body = attrs.innerHtml ?? escapeHtmlForWikiLinkDisplay(attrs.display)
  return `<a href="${attrHref}" class="${attrs.className}" ${WIKI_LINK_PORTABLE_PATH_ATTR}="${attrPortablePath}"${displayAttr}${noteIdAttr}${resolutionAttr}>${body}</a>`
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

function isAmbiguousWikiLink(w: WikiLink): boolean {
  return w.resolution === "AMBIGUOUS"
}

export function wikiLinkAmbiguousResolution(
  wikiLinks: readonly WikiLink[],
  portablePath: string,
  authored: string
): "AMBIGUOUS" | undefined {
  for (const w of wikiLinks) {
    if (!isAmbiguousWikiLink(w)) continue
    if (
      w.portablePath === portablePath ||
      w.portablePath.trim() === portablePath.trim() ||
      w.authoredLink === authored
    ) {
      return "AMBIGUOUS"
    }
  }
  return undefined
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

/** Stored wiki token for the clicked dead wiki link, for replace. */
export function markdownWikiTokenFromDeadWikiLinkPayload(
  p: DeadWikiLinkPayload
): string {
  const { portablePath, displayText } = p
  if (portablePath === displayText) return `[[${portablePath}]]`
  return `[[${portablePath}|${displayText}]]`
}

/** Markdown token for a wiki anchor (dead or live) from DOM; prefers `data-portable-path` / bracketed display. */
export function wikiAnchorToMarkdownToken(anchor: HTMLAnchorElement): string {
  const raw = anchor.textContent?.trim() ?? ""
  const portablePath = anchor.getAttribute(WIKI_LINK_PORTABLE_PATH_ATTR)
  if (portablePath === null || portablePath === "") {
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
    if (displayPart === portablePath) {
      return `[[${portablePath}]]`
    }
    return `[[${portablePath}|${displayPart}]]`
  }

  if (innerM !== null) {
    const visibleInner = innerM[1]!
    if (visibleInner === portablePath) {
      return `[[${portablePath}]]`
    }
    return `[[${portablePath}|${visibleInner}]]`
  }

  if (raw.startsWith("[[") && !raw.endsWith("]]")) {
    return raw
  }

  if (raw === portablePath) {
    return `[[${portablePath}]]`
  }
  return `[[${portablePath}|${raw}]]`
}
