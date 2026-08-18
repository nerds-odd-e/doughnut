import type { WikiTitle } from "@generated/doughnut-backend-api"
import { noteShowHref } from "@/routes/noteShowLocation"
import {
  DEAD_WIKI_LINK_CLASS,
  DOUGHNUT_WIKI_LINK_CLASS,
} from "@/utils/wikiLinkDomMarkers"
import {
  authoredLinkOccurrences,
  splitAuthoredToken,
  splitWikiLinkInner,
} from "@/utils/authoredLinkMarkup"
import {
  escapeHtmlAttributeValue,
  escapeHtmlForWikiLinkDisplay,
  isValidWikiLinkInner,
  wikiAnchorToMarkdownToken,
  wikiLinkAnchorHtml,
  wikiLinkBracketedInnerHtml,
  wikiTitleNoteIdLookup,
} from "@/utils/wikiLinkMarkup"

/**
 * Renders a YAML property scalar with clickable wiki and path-Markdown links.
 * Well-formed wiki `[[title]]` uses bracket UI; path Markdown uses the same
 * live/dead wiki-link classes as body path links (plain display, path href).
 */
export function propertyValuePlainToDisplayHtml(
  plain: string,
  wikiTitles: WikiTitle[]
): string {
  const map = wikiTitleNoteIdLookup(wikiTitles)

  let out = ""
  let lastIndex = 0
  for (const occ of authoredLinkOccurrences(plain)) {
    if (occ.start < lastIndex) continue
    out += escapeHtmlForWikiLinkDisplay(plain.slice(lastIndex, occ.start))
    lastIndex = occ.end
    const fullMatch = plain.slice(occ.start, occ.end)
    if (occ.kind === "pathMarkdown") {
      const { target, display } = splitAuthoredToken(occ.token)
      const noteId = map.get(occ.token.trim()) ?? map.get(target.trim())
      out += wikiLinkAnchorHtml({
        href: target,
        className:
          noteId === undefined
            ? DEAD_WIKI_LINK_CLASS
            : DOUGHNUT_WIKI_LINK_CLASS,
        target,
        display,
        noteId,
      })
      continue
    }
    if (occ.kind !== "wiki" || !isValidWikiLinkInner(occ.token)) {
      out += escapeHtmlForWikiLinkDisplay(fullMatch)
      continue
    }

    const { target, display } = splitWikiLinkInner(occ.token)
    const noteId = map.get(occ.token.trim()) ?? map.get(target.trim())
    const innerHtml = wikiLinkBracketedInnerHtml(display)
    const attrTarget = escapeHtmlAttributeValue(target)
    const displayAttr =
      display !== target
        ? ` data-wiki-display="${escapeHtmlAttributeValue(display)}"`
        : ""
    if (noteId !== undefined) {
      out += `<a href="${noteShowHref(noteId)}" class="${DOUGHNUT_WIKI_LINK_CLASS}" data-wiki-title="${attrTarget}"${displayAttr}>${innerHtml}</a>`
    } else {
      out += `<a href="#" class="${DEAD_WIKI_LINK_CLASS}" data-wiki-title="${attrTarget}"${displayAttr}>${innerHtml}</a>`
    }
  }
  out += escapeHtmlForWikiLinkDisplay(plain.slice(lastIndex))
  return out
}

/** Serializes the editor root (top-level nodes) back to a plain scalar. Wiki anchors use visible text only (so in-place edits are saved). */
export function serializePropertyValueFieldRoot(el: HTMLElement): string {
  let out = ""
  for (const node of el.childNodes) {
    if (node.nodeType === Node.TEXT_NODE) {
      out += node.textContent ?? ""
    } else if (node instanceof HTMLBRElement) {
      continue
    } else if (node instanceof HTMLAnchorElement) {
      if (
        node.classList.contains(DOUGHNUT_WIKI_LINK_CLASS) ||
        node.classList.contains(DEAD_WIKI_LINK_CLASS)
      ) {
        out += wikiAnchorToMarkdownToken(node)
        continue
      }
      out += node.textContent ?? ""
    } else if (node instanceof HTMLElement) {
      out += node.textContent ?? ""
    }
  }
  return out.replace(/\r?\n/g, "")
}
