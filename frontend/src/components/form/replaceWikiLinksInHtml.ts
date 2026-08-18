import type { WikiTitle } from "@generated/doughnut-backend-api"
import { noteShowHref } from "@/routes/noteShowLocation"
import {
  DEAD_WIKI_LINK_CLASS,
  DOUGHNUT_WIKI_LINK_CLASS,
} from "@/utils/wikiLinkDomMarkers"
import {
  escapeHtmlAttributeValue,
  escapeHtmlForWikiLinkDisplay,
  isPathMarkdownWikiTitle,
  isValidWikiLinkInner,
  splitWikiLinkInner,
  wikiTitleParts,
} from "@/utils/wikiLinkMarkup"

/** Visible inner text of a dead-wiki-link anchor (bracket UI or plain). */
function deadWikiLinkBracketDisplayMatches(
  anchor: Element,
  display: string
): boolean {
  const raw = anchor.textContent?.trim() ?? ""
  const innerM = /^\[\[(.*)\]\]$/.exec(raw)
  const visibleInner = innerM?.[1] !== undefined ? innerM[1].trim() : raw
  return visibleInner === display.trim()
}

/** Rich editor HTML uses dead-wiki-link anchors, not [[ ]] literals; upgrade when titles resolve. */
function upgradeDeadWikiAnchors(html: string, wikiTitles: WikiTitle[]): string {
  if (wikiTitles.length === 0 || !html.includes(DEAD_WIKI_LINK_CLASS)) {
    return html
  }
  const parser = new DOMParser()
  const doc = parser.parseFromString(
    `<div id="doughnut-wiki-upgrade-wrap">${html}</div>`,
    "text/html"
  )
  const wrap = doc.getElementById("doughnut-wiki-upgrade-wrap")
  if (!wrap) return html

  for (const w of wikiTitles) {
    const { target, display } = wikiTitleParts(w)
    const href = noteShowHref(w.noteId)
    for (const a of [...wrap.querySelectorAll(`a.${DEAD_WIKI_LINK_CLASS}`)]) {
      const dt = a.getAttribute("data-wiki-title")
      if (dt !== null && dt !== "") {
        if (dt !== target && dt.trim() !== target.trim()) continue
        if (!deadWikiLinkBracketDisplayMatches(a, display)) continue
      } else if (!deadWikiLinkBracketDisplayMatches(a, display)) {
        continue
      }
      const live = doc.createElement("a")
      live.setAttribute("href", href)
      live.className = DOUGHNUT_WIKI_LINK_CLASS
      live.setAttribute("data-wiki-title", target)
      if (display !== target) {
        live.setAttribute("data-wiki-display", display)
      }
      live.textContent = display
      a.replaceWith(live)
    }
  }
  return wrap.innerHTML
}

function deadWikiAnchorHtmlFromInner(innerRaw: string): string {
  if (!isValidWikiLinkInner(innerRaw)) {
    return escapeHtmlForWikiLinkDisplay(`[[${innerRaw}]]`)
  }
  const { target, display } = splitWikiLinkInner(innerRaw)
  const attrTarget = escapeHtmlAttributeValue(target)
  const displayAttr =
    display !== target
      ? ` data-wiki-display="${escapeHtmlAttributeValue(display)}"`
      : ""
  return `<a href="#" class="${DEAD_WIKI_LINK_CLASS}" data-wiki-title="${attrTarget}"${displayAttr}>${escapeHtmlForWikiLinkDisplay(display)}</a>`
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
    const displayAttr =
      display !== target
        ? ` data-wiki-display="${escapeHtmlAttributeValue(display)}"`
        : ""
    const live = `<a href="${attrTarget}" class="${DOUGHNUT_WIKI_LINK_CLASS}" data-wiki-title="${attrTarget}"${displayAttr} data-note-id="${w.noteId}">${escapeHtmlForWikiLinkDisplay(display)}</a>`
    result = result.replaceAll(`<a href="${attrTarget}">${display}</a>`, live)
  }
  return result
}

export function replaceWikiLinksInHtml(
  html: string,
  wikiTitles: WikiTitle[]
): string {
  let result = html
  wikiTitles.forEach((w) => {
    if (isPathMarkdownWikiTitle(w)) return
    const { target, display, inner } = wikiTitleParts(w)
    const attrTarget = escapeHtmlAttributeValue(target)
    const displayAttr =
      display !== target
        ? ` data-wiki-display="${escapeHtmlAttributeValue(display)}"`
        : ""
    result = result.replaceAll(
      `[[${inner}]]`,
      `<a href="${noteShowHref(w.noteId)}" class="${DOUGHNUT_WIKI_LINK_CLASS}" data-wiki-title="${attrTarget}"${displayAttr}>${escapeHtmlForWikiLinkDisplay(display)}</a>`
    )
  })
  result = upgradePathMarkdownAnchors(result, wikiTitles)
  result = upgradeDeadWikiAnchors(result, wikiTitles)
  result = result.replace(
    /\[\[([^\[\]\r\n]*)\]\]/g,
    (_fullMatch, inner: string) => deadWikiAnchorHtmlFromInner(inner)
  )
  return result
}
