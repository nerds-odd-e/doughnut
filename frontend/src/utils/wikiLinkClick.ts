import type { RouteLocationRaw } from "vue-router"
import { authoredHrefLooksLikePortablePath } from "@/utils/authoredLinkMarkup"
import {
  DEAD_WIKI_LINK_CLASS,
  DONUT_WIKI_LINK_CLASS,
  PENDING_WIKI_LINK_CLASS,
  WIKI_LINK_DISPLAY_TEXT_ATTR,
  WIKI_LINK_PORTABLE_PATH_ATTR,
  WIKI_LINK_RESOLUTION_ATTR,
} from "@/utils/wikiLinkDomMarkers"
import { locationForResolvedWikiTarget } from "@/utils/wikiLinkResolvedLocation"

/** Dead wiki link click payload containing the target token and visible display text. */
export type DeadWikiLinkPayload = {
  portablePath: string
  displayText: string
  resolution?: "AMBIGUOUS"
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
  const displayText =
    displayAttr !== null && displayAttr !== "" ? displayAttr : portablePath
  const resolution =
    anchor.getAttribute(WIKI_LINK_RESOLUTION_ATTR) === "AMBIGUOUS"
      ? ("AMBIGUOUS" as const)
      : undefined
  return resolution === undefined
    ? { portablePath, displayText }
    : { portablePath, displayText, resolution }
}
