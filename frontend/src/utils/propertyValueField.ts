import type { WikiTitle } from "@generated/doughnut-backend-api"
import { noteShowHref } from "@/routes/noteShowLocation"
import {
  DEAD_WIKI_LINK_CLASS,
  DOUGHNUT_WIKI_LINK_CLASS,
} from "@/utils/wikiLinkDomMarkers"
import {
  escapeHtmlAttributeValue,
  escapeHtmlForWikiLinkDisplay,
  isValidWikiLinkInner,
  splitWikiLinkInner,
  wikiAnchorToMarkdownToken,
  wikiLinkBracketedInnerHtml,
  wikiTitleNoteIdLookup,
} from "@/utils/wikiLinkMarkup"

/**
 * Renders a YAML property scalar with clickable wiki links. Only well-formed `[[title]]` segments
 * (non-empty title, no `[`/`]`/newlines inside) become links; everything else stays plain text.
 */
export function propertyValuePlainToDisplayHtml(
  plain: string,
  wikiTitles: WikiTitle[]
): string {
  const map = wikiTitleNoteIdLookup(wikiTitles)

  const re = /\[\[([^\[\]\r\n]*)\]\]/g
  let out = ""
  let lastIndex = 0
  let m: RegExpExecArray | null
  while ((m = re.exec(plain)) !== null) {
    const fullMatch = m[0]
    const titleRaw = m[1] ?? ""
    const start = m.index

    out += escapeHtmlForWikiLinkDisplay(plain.slice(lastIndex, start))
    lastIndex = start + fullMatch.length

    if (!isValidWikiLinkInner(titleRaw)) {
      out += escapeHtmlForWikiLinkDisplay(fullMatch)
      continue
    }

    const { target, display } = splitWikiLinkInner(titleRaw)
    const noteId = map.get(titleRaw.trim()) ?? map.get(target.trim())
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
