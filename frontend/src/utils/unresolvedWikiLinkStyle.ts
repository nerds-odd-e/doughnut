import { authoredLinkOccurrences } from "@/utils/authoredLinkMarkup"
import {
  DEAD_WIKI_LINK_CLASS,
  PENDING_WIKI_LINK_CLASS,
} from "@/utils/wikiLinkDomMarkers"

/** Authored tokens from the last persisted markdown, or undefined when that snapshot is unknown. */
export function lastSavedAuthoredTokens(
  lastSavedMarkdown: string | undefined
): Set<string> | undefined {
  if (lastSavedMarkdown === undefined) return undefined
  return new Set(authoredLinkOccurrences(lastSavedMarkdown).map((o) => o.token))
}

/** Unresolved wiki/path token: dead if last-saved is unknown or already contains it; otherwise pending. */
export function unresolvedWikiClass(
  token: string,
  lastSavedTokens: Set<string> | undefined
): string {
  if (lastSavedTokens === undefined || lastSavedTokens.has(token)) {
    return DEAD_WIKI_LINK_CLASS
  }
  return PENDING_WIKI_LINK_CLASS
}
