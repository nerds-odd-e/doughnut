import type { WikiTitle } from "@generated/doughnut-backend-api"
import { noteShowHref } from "@/routes/noteShowLocation"
import {
  escapeHtmlAttributeValue,
  escapeHtmlForWikiPropertyValue,
  isValidPropertyWikiInner,
  splitWikiLinkInner,
  wikiLinkBracketedInnerHtml,
} from "@/utils/wikiPropertyValueField"

function deadLinkVisibleInnerMatchesLinkText(
  anchor: Element,
  linkText: string
): boolean {
  const raw = anchor.textContent?.trim() ?? ""
  const innerM = /^\[\[(.*)\]\]$/.exec(raw)
  const visibleInner = innerM?.[1] !== undefined ? innerM[1].trim() : raw
  return visibleInner === linkText.trim()
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

  for (const { linkText, noteId } of wikiTitles) {
    const href = noteShowHref(noteId)
    for (const a of [...wrap.querySelectorAll("a.dead-link")]) {
      const dt = a.getAttribute("data-wiki-title")
      if (dt !== null && dt !== "") {
        if (dt !== linkText && dt.trim() !== linkText.trim()) continue
        if (!deadLinkVisibleInnerMatchesLinkText(a, linkText)) continue
      } else if (!deadLinkVisibleInnerMatchesLinkText(a, linkText)) {
        const raw = a.textContent?.trim() ?? ""
        if (raw !== linkText) continue
      }
      const live = doc.createElement("a")
      live.setAttribute("href", href)
      live.className = "doughnut-link"
      live.textContent = linkText
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
  return `<a href="#" class="dead-link" data-wiki-title="${attrTarget}"${displayAttr}>${wikiLinkBracketedInnerHtml(display)}</a>`
}

export function replaceWikiLinksInHtml(
  html: string,
  wikiTitles: WikiTitle[]
): string {
  let result = html
  wikiTitles.forEach(({ linkText, noteId }) => {
    result = result.replaceAll(
      `[[${linkText}]]`,
      `<a href="${noteShowHref(noteId)}" class="doughnut-link">${linkText}</a>`
    )
  })
  result = upgradeDeadWikiAnchors(result, wikiTitles)
  result = result.replace(
    /\[\[([^\[\]\r\n]*)\]\]/g,
    (_fullMatch, inner: string) => deadWikiAnchorHtmlFromInner(inner)
  )
  return result
}
