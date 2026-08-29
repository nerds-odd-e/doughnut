import type { WikiTitle } from "@generated/donut-backend-api"
import { noteShowHref } from "@/routes/noteShowLocation"
import {
  authoredLinkOccurrences,
  noteIdForAuthoredToken,
  splitAuthoredToken,
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
  wikiTitleNoteIdLookup,
} from "@/utils/wikiLinkMarkup"

/**
 * Renders a YAML property scalar with clickable wiki and path-Markdown links.
 * Well-formed wiki `[[title]]` uses bracket UI; path Markdown uses the same
 * live/dead/pending wiki-link classes as body path links (plain display).
 */
export function propertyValuePlainToDisplayHtml(
  plain: string,
  wikiTitles: WikiTitle[],
  lastSavedMarkdown?: string
): string {
  const map = wikiTitleNoteIdLookup(wikiTitles)
  const lastSavedTokens = lastSavedAuthoredTokens(lastSavedMarkdown)

  let out = ""
  let lastIndex = 0
  for (const occ of authoredLinkOccurrences(plain)) {
    if (occ.start < lastIndex) continue
    out += escapeHtmlForWikiLinkDisplay(plain.slice(lastIndex, occ.start))
    lastIndex = occ.end
    const fullMatch = plain.slice(occ.start, occ.end)
    if (occ.kind === "pathMarkdown") {
      const { target, display } = splitAuthoredToken(occ.token)
      const noteId = noteIdForAuthoredToken(occ.token, map)
      out += wikiLinkAnchorHtml({
        href: noteId !== undefined ? noteShowHref(noteId) : "#",
        className:
          noteId === undefined
            ? unresolvedWikiClass(occ.token, lastSavedTokens)
            : DONUT_WIKI_LINK_CLASS,
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
    const noteId = noteIdForAuthoredToken(occ.token, map)
    out += wikiLinkAnchorHtml({
      href: noteId === undefined ? "#" : noteShowHref(noteId),
      className:
        noteId === undefined
          ? unresolvedWikiClass(occ.token, lastSavedTokens)
          : DONUT_WIKI_LINK_CLASS,
      target,
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
