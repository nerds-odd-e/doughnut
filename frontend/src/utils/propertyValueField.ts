import type { WikiLink } from "@generated/donut-backend-api"
import {
  authoredLinkOccurrences,
  noteIdForAuthoredToken,
  splitWikiLinkInner,
} from "@/utils/authoredLinkMarkup"
import {
  lastSavedAuthoredTokens,
  unresolvedWikiClass,
} from "@/utils/unresolvedWikiLinkStyle"
import {
  DONUT_WIKI_LINK_CLASS,
  isWikiLinkAnchor,
} from "@/utils/wikiLinkDomMarkers"
import {
  escapeHtmlForWikiLinkDisplay,
  isValidWikiLinkInner,
  wikiAnchorToMarkdownToken,
  wikiLinkAnchorHtml,
  wikiLinkBracketedInnerHtml,
  wikiLinkNoteIdLookup,
} from "@/utils/wikiLinkMarkup"
import { hrefForResolvedWikiTarget } from "@/utils/wikiLinkResolvedLocation"

/**
 * Renders a YAML property scalar with clickable wiki links.
 * Well-formed wiki `[[title]]` uses bracket UI; ordinary Markdown stays plain text.
 */
export function propertyValuePlainToDisplayHtml(
  plain: string,
  wikiLinks: WikiLink[],
  lastSavedMarkdown?: string
): string {
  const map = wikiLinkNoteIdLookup(wikiLinks)
  const lastSavedTokens = lastSavedAuthoredTokens(lastSavedMarkdown)

  let out = ""
  let lastIndex = 0
  for (const occ of authoredLinkOccurrences(plain)) {
    if (occ.start < lastIndex) continue
    out += escapeHtmlForWikiLinkDisplay(plain.slice(lastIndex, occ.start))
    lastIndex = occ.end
    const fullMatch = plain.slice(occ.start, occ.end)
    if (!isValidWikiLinkInner(occ.token)) {
      out += escapeHtmlForWikiLinkDisplay(fullMatch)
      continue
    }

    const { target, display } = splitWikiLinkInner(occ.token)
    const noteId = noteIdForAuthoredToken(occ.token, map)
    out += wikiLinkAnchorHtml({
      href:
        noteId === undefined ? "#" : hrefForResolvedWikiTarget(noteId, target),
      className:
        noteId === undefined
          ? unresolvedWikiClass(occ.token, lastSavedTokens)
          : DONUT_WIKI_LINK_CLASS,
      portablePath: target,
      display,
      noteId,
      innerHtml: wikiLinkBracketedInnerHtml(display),
    })
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
      if (isWikiLinkAnchor(node)) {
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
