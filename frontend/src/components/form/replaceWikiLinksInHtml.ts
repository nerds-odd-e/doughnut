import type { WikiLink } from "@generated/donut-backend-api"
import {
  authoredHrefLooksLikePortablePath,
  isPathMarkdownWikiLink,
} from "@/utils/authoredLinkMarkup"
import {
  DEAD_WIKI_LINK_CLASS,
  DONUT_WIKI_LINK_CLASS,
  PENDING_WIKI_LINK_CLASS,
  WIKI_LINK_DISPLAY_TEXT_ATTR,
  WIKI_LINK_PORTABLE_PATH_ATTR,
} from "@/utils/wikiLinkDomMarkers"
import {
  lastSavedAuthoredTokens,
  unresolvedWikiClass,
} from "@/utils/unresolvedWikiLinkStyle"
import {
  escapeHtmlAttributeValue,
  escapeHtmlForWikiLinkDisplay,
  isResolvedWikiLink,
  isValidWikiLinkInner,
  splitWikiLinkInner,
  wikiLinkAmbiguousResolution,
  wikiLinkAnchorHtml,
} from "@/utils/wikiLinkMarkup"
import { hrefForResolvedWikiTarget } from "@/utils/wikiLinkResolvedLocation"

const UNRESOLVED_WIKI_LINK_CLASSES = [
  DEAD_WIKI_LINK_CLASS,
  PENDING_WIKI_LINK_CLASS,
] as const

function authoredTokenFromWikiAnchor(anchor: Element): string {
  const portablePath = anchor.getAttribute(WIKI_LINK_PORTABLE_PATH_ATTR) ?? ""
  if (authoredHrefLooksLikePortablePath(portablePath)) {
    const display =
      anchor.getAttribute(WIKI_LINK_DISPLAY_TEXT_ATTR) ||
      anchor.textContent?.trim() ||
      ""
    return `[${display}](${portablePath})`
  }
  const display = anchor.getAttribute(WIKI_LINK_DISPLAY_TEXT_ATTR)
  if (display !== null && display !== "" && display !== portablePath) {
    return `${portablePath}|${display}`
  }
  return portablePath
}

/** Visible inner text of a wiki-link anchor (bracket UI or plain). */
function wikiAnchorDisplayMatches(anchor: Element, display: string): boolean {
  const raw = anchor.textContent?.trim() ?? ""
  const innerM = /^\[\[(.*)\]\]$/.exec(raw)
  const visibleInner = innerM?.[1] !== undefined ? innerM[1].trim() : raw
  return visibleInner === display.trim()
}

function parseWikiHtmlFragment(html: string): HTMLElement | undefined {
  const parser = new DOMParser()
  const doc = parser.parseFromString(
    `<div id="donut-wiki-html-wrap">${html}</div>`,
    "text/html"
  )
  const wrap = doc.getElementById("donut-wiki-html-wrap")
  if (!wrap) return undefined
  return wrap
}

/** Rich editor HTML uses dead/pending wiki-link anchors, not [[ ]] literals; upgrade when titles resolve. */
function upgradeUnresolvedWikiAnchors(
  html: string,
  wikiLinks: WikiLink[]
): string {
  if (wikiLinks.length === 0) return html
  if (!UNRESOLVED_WIKI_LINK_CLASSES.some((c) => html.includes(c))) return html
  const wrap = parseWikiHtmlFragment(html)
  if (!wrap) return html
  const unresolvedAnchorSelector = UNRESOLVED_WIKI_LINK_CLASSES.map(
    (c) => `a.${c}`
  ).join(", ")

  for (const w of wikiLinks) {
    if (!isResolvedWikiLink(w) || isPathMarkdownWikiLink(w)) continue
    const href = hrefForResolvedWikiTarget(w.destinationNoteId, w.portablePath)
    for (const a of [...wrap.querySelectorAll(unresolvedAnchorSelector)]) {
      const portablePath = a.getAttribute(WIKI_LINK_PORTABLE_PATH_ATTR)
      if (portablePath !== null && portablePath !== "") {
        if (
          portablePath !== w.portablePath &&
          portablePath.trim() !== w.portablePath.trim()
        )
          continue
        if (!wikiAnchorDisplayMatches(a, w.displayText)) continue
      } else if (!wikiAnchorDisplayMatches(a, w.displayText)) {
        continue
      }
      a.outerHTML = wikiLinkAnchorHtml({
        href,
        className: DONUT_WIKI_LINK_CLASS,
        portablePath: w.portablePath,
        display: w.displayText,
        noteId: w.destinationNoteId,
      })
    }
  }
  return wrap.innerHTML
}

/** Pending anchors whose token is now in last-saved markdown become dead. */
function confirmPendingWikiAnchorsAsDead(
  html: string,
  lastSavedTokens: Set<string> | undefined
): string {
  if (
    lastSavedTokens === undefined ||
    lastSavedTokens.size === 0 ||
    !html.includes(PENDING_WIKI_LINK_CLASS)
  ) {
    return html
  }
  const wrap = parseWikiHtmlFragment(html)
  if (!wrap) return html

  for (const a of [...wrap.querySelectorAll(`a.${PENDING_WIKI_LINK_CLASS}`)]) {
    a.className = unresolvedWikiClass(
      authoredTokenFromWikiAnchor(a),
      lastSavedTokens
    )
  }
  return wrap.innerHTML
}

function unresolvedWikiAnchorHtmlFromInner(
  innerRaw: string,
  lastSavedTokens: Set<string> | undefined,
  wikiLinks: WikiLink[]
): string {
  if (!isValidWikiLinkInner(innerRaw)) {
    return escapeHtmlForWikiLinkDisplay(`[[${innerRaw}]]`)
  }
  const { target, display } = splitWikiLinkInner(innerRaw)
  return wikiLinkAnchorHtml({
    href: "#",
    className: unresolvedWikiClass(innerRaw, lastSavedTokens),
    portablePath: target,
    display,
    resolution: wikiLinkAmbiguousResolution(wikiLinks, target, innerRaw),
  })
}

function upgradePathMarkdownAnchors(
  html: string,
  wikiLinks: WikiLink[]
): string {
  let result = html
  for (const w of wikiLinks) {
    if (!isResolvedWikiLink(w) || !isPathMarkdownWikiLink(w)) continue
    const attrPortablePath = escapeHtmlAttributeValue(w.portablePath)
    const livePathMarkdownAttrs = {
      className: DONUT_WIKI_LINK_CLASS,
      portablePath: w.portablePath,
      display: w.displayText,
      noteId: w.destinationNoteId,
    }
    const live = wikiLinkAnchorHtml({
      href: hrefForResolvedWikiTarget(w.destinationNoteId, w.portablePath),
      ...livePathMarkdownAttrs,
    })
    result = result.replaceAll(
      `<a href="${attrPortablePath}">${w.displayText}</a>`,
      live
    )
    const leftoverUnresolvedHrefs = [w.portablePath, "#"]
    for (const leftoverHref of leftoverUnresolvedHrefs) {
      for (const className of UNRESOLVED_WIKI_LINK_CLASSES) {
        result = result.replaceAll(
          wikiLinkAnchorHtml({
            href: leftoverHref,
            className,
            portablePath: w.portablePath,
            display: w.displayText,
          }),
          live
        )
      }
    }
    result = result.replaceAll(
      wikiLinkAnchorHtml({ href: w.portablePath, ...livePathMarkdownAttrs }),
      live
    )
  }
  return result
}

/** Leftover `[[…]]` and leftover portable-path hrefs get pending or dead wiki-link UI. */
function markUnresolvedWikiLinks(
  html: string,
  wikiLinks: WikiLink[],
  lastSavedTokens: Set<string> | undefined
): string {
  const withWikiTokens = html.replace(
    /\[\[([^\[\]\r\n]*)\]\]/g,
    (_fullMatch, inner: string) =>
      unresolvedWikiAnchorHtmlFromInner(inner, lastSavedTokens, wikiLinks)
  )
  return withWikiTokens.replace(
    /<a href="(\/[^"]+)">([^<]*)<\/a>/g,
    (full, href: string, display: string) => {
      if (!authoredHrefLooksLikePortablePath(href)) return full
      const token = `[${display}](${href})`
      return wikiLinkAnchorHtml({
        href: "#",
        className: unresolvedWikiClass(token, lastSavedTokens),
        portablePath: href,
        display,
        resolution: wikiLinkAmbiguousResolution(wikiLinks, href, token),
      })
    }
  )
}

export function replaceWikiLinksInHtml(
  html: string,
  wikiLinks: WikiLink[],
  lastSavedMarkdown?: string
): string {
  const lastSavedTokens = lastSavedAuthoredTokens(lastSavedMarkdown)
  let result = html
  wikiLinks.forEach((w) => {
    if (!isResolvedWikiLink(w) || isPathMarkdownWikiLink(w)) return
    result = result.replaceAll(
      `[[${w.authoredLink}]]`,
      wikiLinkAnchorHtml({
        href: hrefForResolvedWikiTarget(w.destinationNoteId, w.portablePath),
        className: DONUT_WIKI_LINK_CLASS,
        portablePath: w.portablePath,
        display: w.displayText,
        noteId: w.destinationNoteId,
      })
    )
  })
  result = upgradePathMarkdownAnchors(result, wikiLinks)
  result = upgradeUnresolvedWikiAnchors(result, wikiLinks)
  result = confirmPendingWikiAnchorsAsDead(result, lastSavedTokens)
  return markUnresolvedWikiLinks(result, wikiLinks, lastSavedTokens)
}
