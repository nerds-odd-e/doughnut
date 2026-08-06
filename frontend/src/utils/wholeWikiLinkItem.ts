/**
 * Whole-item wiki-link token — mirrors WikiLinkMarkdown.INNER_LINK_PATTERN.matches()
 * plus non-empty target after optional display pipe.
 */
import { splitWikiLinkInner } from "@/utils/wikiPropertyValueField"

const WHOLE_WIKI_LINK = /^\[\[([^\]]+)]]$/

export function parseWholeWikiLinkItem(
  trimmed: string
): { inner: string; target: string; display: string } | undefined {
  const match = WHOLE_WIKI_LINK.exec(trimmed)
  const captured = match?.[1]
  if (captured === undefined) return
  const inner = captured.trim()
  if (inner === "") return
  const { target, display } = splitWikiLinkInner(inner)
  if (target.trim() === "") return
  return { inner, target, display }
}

export function isWellFormedWholeWikiLinkItem(trimmed: string): boolean {
  return parseWholeWikiLinkItem(trimmed) !== undefined
}
