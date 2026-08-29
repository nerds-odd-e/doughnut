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
import {
  lastSavedAuthoredTokens,
  unresolvedWikiClass,
} from "@/utils/unresolvedWikiLinkStyle"
import {
  escapeHtmlAttributeValue,
  escapeHtmlForWikiLinkDisplay,
  isPathMarkdownWikiTitle,
  isValidWikiLinkInner,
  splitWikiLinkInner,
  wikiLinkAnchorHtml,
  wikiTitleParts,
} from "@/utils/wikiLinkMarkup"

function authoredTokenFromWikiAnchor(anchor: Element): string {
  const target = anchor.getAttribute("data-wiki-title") ?? ""
  if (hrefLooksLikeConceptNotePath(target)) {
    const display =
      anchor.getAttribute("data-wiki-display") ||
      anchor.textContent?.trim() ||
      ""
    return `[${display}](${target})`
  }
  const display = anchor.getAttribute("data-wiki-display")
  if (display !== null && display !== "" && display !== target) {
    return `${target}|${display}`
  }
  return target
}

/** Visible inner text of a wiki-link anchor (bracket UI or plain). */
function wikiAnchorDisplayMatches(anchor: Element, display: string): boolean {
  const raw = anchor.textContent?.trim() ?? ""
  const innerM = /^\[\[(.*)\]\]$/.exec(raw)
  const visibleInner = innerM?.[1] !== undefined ? innerM[1].trim() : raw
  return visibleInner === display.trim()
}

function parseWikiHtmlFragment(
  html: string
): { wrap: HTMLElement; doc: Document } | undefined {
  const parser = new DOMParser()
  const doc = parser.parseFromString(
    `<div id="donut-wiki-html-wrap">${html}</div>`,
    "text/html"
  )
  const wrap = doc.getElementById("donut-wiki-html-wrap")
  if (!wrap) return undefined
  return { wrap, doc }
}

/** Rich editor HTML uses dead/pending wiki-link anchors, not [[ ]] literals; upgrade when titles resolve. */
function upgradeUnresolvedWikiAnchors(
  html: string,
  wikiTitles: WikiTitle[]
): string {
  if (wikiTitles.length === 0) return html
  const unresolvedClasses = [DEAD_WIKI_LINK_CLASS, PENDING_WIKI_LINK_CLASS]
  if (!unresolvedClasses.some((c) => html.includes(c))) return html
  const parsed = parseWikiHtmlFragment(html)
  if (!parsed) return html
  const { wrap } = parsed
  const unresolvedAnchorSelector = unresolvedClasses
    .map((c) => `a.${c}`)
    .join(", ")

  for (const w of wikiTitles) {
    if (isPathMarkdownWikiTitle(w)) continue
    const { target, display } = wikiTitleParts(w)
    const href = noteShowHref(w.noteId)
    for (const a of [...wrap.querySelectorAll(unresolvedAnchorSelector)]) {
      const dt = a.getAttribute("data-wiki-title")
      if (dt !== null && dt !== "") {
        if (dt !== target && dt.trim() !== target.trim()) continue
        if (!wikiAnchorDisplayMatches(a, display)) continue
      } else if (!wikiAnchorDisplayMatches(a, display)) {
        continue
      }
      a.outerHTML = wikiLinkAnchorHtml({
        href,
        className: DONUT_WIKI_LINK_CLASS,
        target,
        display,
        noteId: w.noteId,
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
  const parsed = parseWikiHtmlFragment(html)
  if (!parsed) return html

  for (const a of [
    ...parsed.wrap.querySelectorAll(`a.${PENDING_WIKI_LINK_CLASS}`),
  ]) {
    a.className = unresolvedWikiClass(
      authoredTokenFromWikiAnchor(a),
      lastSavedTokens
    )
  }
  return parsed.wrap.innerHTML
}

function unresolvedWikiAnchorHtmlFromInner(
  innerRaw: string,
  lastSavedTokens: Set<string> | undefined
): string {
  if (!isValidWikiLinkInner(innerRaw)) {
    return escapeHtmlForWikiLinkDisplay(`[[${innerRaw}]]`)
  }
  const { target, display } = splitWikiLinkInner(innerRaw)
  return wikiLinkAnchorHtml({
    href: "#",
    className: unresolvedWikiClass(innerRaw, lastSavedTokens),
    target,
    display,
  })
}

function upgradePathMarkdownAnchors(
  html: string,
  wikiTitles: WikiTitle[]
): string {
  let result = html
  for (const w of wikiTitles) {
    if (!isPathMarkdownWikiTitle(w)) continue
    const { target, display } = wikiTitleParts(w)
    const attrTarget = escapeHtmlAttributeValue(target)
    const livePathMarkdownAttrs = {
      className: DONUT_WIKI_LINK_CLASS,
      target,
      display,
      noteId: w.noteId,
    }
    const live = wikiLinkAnchorHtml({
      href: noteShowHref(w.noteId),
      ...livePathMarkdownAttrs,
    })
    result = result.replaceAll(`<a href="${attrTarget}">${display}</a>`, live)
    result = result.replaceAll(
      wikiLinkAnchorHtml({
        href: target,
        className: DEAD_WIKI_LINK_CLASS,
        target,
        display,
      }),
      live
    )
    result = result.replaceAll(
      wikiLinkAnchorHtml({ href: target, ...livePathMarkdownAttrs }),
      live
    )
  }
  return result
}

/** Leftover `[[…]]` and leftover concept-path hrefs get pending or dead wiki-link UI. */
function markUnresolvedWikiLinks(
  html: string,
  lastSavedTokens: Set<string> | undefined
): string {
  const withWikiTokens = html.replace(
    /\[\[([^\[\]\r\n]*)\]\]/g,
    (_fullMatch, inner: string) =>
      unresolvedWikiAnchorHtmlFromInner(inner, lastSavedTokens)
  )
  return withWikiTokens.replace(
    /<a href="(\/[^"]+)">([^<]*)<\/a>/g,
    (full, href: string, display: string) => {
      if (!hrefLooksLikeConceptNotePath(href)) return full
      const token = `[${display}](${href})`
      return wikiLinkAnchorHtml({
        href,
        className: unresolvedWikiClass(token, lastSavedTokens),
        target: href,
        display,
      })
    }
  )
}

export function replaceWikiLinksInHtml(
  html: string,
  wikiTitles: WikiTitle[],
  lastSavedMarkdown?: string
): string {
  const lastSavedTokens = lastSavedAuthoredTokens(lastSavedMarkdown)
  let result = html
  wikiTitles.forEach((w) => {
    if (isPathMarkdownWikiTitle(w)) return
    const { target, display, inner } = wikiTitleParts(w)
    result = result.replaceAll(
      `[[${inner}]]`,
      wikiLinkAnchorHtml({
        href: noteShowHref(w.noteId),
        className: DONUT_WIKI_LINK_CLASS,
        target,
        display,
        noteId: w.noteId,
      })
    )
  })
  result = upgradePathMarkdownAnchors(result, wikiTitles)
  result = upgradeUnresolvedWikiAnchors(result, wikiTitles)
  result = confirmPendingWikiAnchorsAsDead(result, lastSavedTokens)
  return markUnresolvedWikiLinks(result, lastSavedTokens)
}
