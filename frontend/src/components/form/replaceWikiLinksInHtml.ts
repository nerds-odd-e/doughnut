import type { WikiTitle } from "@generated/doughnut-backend-api"
import { noteShowHref } from "@/routes/noteShowLocation"
import {
  escapeHtmlAttributeValue,
  escapeHtmlForWikiPropertyValue,
  isValidPropertyWikiInner,
  splitWikiLinkInner,
  wikiTitleParts,
} from "@/utils/wikiPropertyValueField"

/** Visible inner text of a dead-link anchor (bracket UI or plain). */
function deadLinkBracketDisplayMatches(
  anchor: Element,
  display: string
): boolean {
  const raw = anchor.textContent?.trim() ?? ""
  const innerM = /^\[\[(.*)\]\]$/.exec(raw)
  const visibleInner = innerM?.[1] !== undefined ? innerM[1].trim() : raw
  return visibleInner === display.trim()
}

/** Rich editor HTML uses dead-link anchors, not [[ ]] literals; upgrade when titles resolve. */
function upgradeDeadWikiAnchors(html: string, wikiTitles: WikiTitle[]): string {
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
    for (const a of [...wrap.querySelectorAll("a.dead-link")]) {
      const dt = a.getAttribute("data-wiki-title")
      if (dt !== null && dt !== "") {
        if (dt !== target && dt.trim() !== target.trim()) continue
        if (!deadLinkBracketDisplayMatches(a, display)) continue
      } else if (!deadLinkBracketDisplayMatches(a, display)) {
        continue
      }
      const live = doc.createElement("a")
      live.setAttribute("href", href)
      live.className = "doughnut-link"
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
  if (!isValidPropertyWikiInner(innerRaw)) {
    return escapeHtmlForWikiPropertyValue(`[[${innerRaw}]]`)
  }
  const { target, display } = splitWikiLinkInner(innerRaw)
  const attrTarget = escapeHtmlAttributeValue(target)
  const displayAttr =
    display !== target
      ? ` data-wiki-display="${escapeHtmlAttributeValue(display)}"`
      : ""
  return `<a href="#" class="dead-link" data-wiki-title="${attrTarget}"${displayAttr}>${escapeHtmlForWikiPropertyValue(display)}</a>`
}

export function replaceWikiLinksInHtml(
  html: string,
  wikiTitles: WikiTitle[]
): string {
  let result = html
  wikiTitles.forEach((w) => {
    const { target, display, inner } = wikiTitleParts(w)
    const attrTarget = escapeHtmlAttributeValue(target)
    const displayAttr =
      display !== target
        ? ` data-wiki-display="${escapeHtmlAttributeValue(display)}"`
        : ""
    result = result.replaceAll(
      `[[${inner}]]`,
      `<a href="${noteShowHref(w.noteId)}" class="doughnut-link" data-wiki-title="${attrTarget}"${displayAttr}>${escapeHtmlForWikiPropertyValue(display)}</a>`
    )
  })
  result = upgradeDeadWikiAnchors(result, wikiTitles)
  result = result.replace(
    /\[\[([^\[\]\r\n]*)\]\]/g,
    (_fullMatch, inner: string) => deadWikiAnchorHtmlFromInner(inner)
  )
  return result
}
