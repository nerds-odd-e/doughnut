/**
 * Whole-item wiki-link token — mirrors WikiLinkMarkdown.INNER_LINK_PATTERN.matches()
 * plus non-empty target after optional display pipe.
 */
const WHOLE_WIKI_LINK = /^\[\[([^\]]+)]]$/

export function isWellFormedWholeWikiLinkItem(trimmed: string): boolean {
  const match = WHOLE_WIKI_LINK.exec(trimmed)
  const captured = match?.[1]
  if (captured === undefined) return false
  const inner = captured.trim()
  if (inner === "") return false
  const pipe = inner.indexOf("|")
  const target = (pipe === -1 ? inner : inner.slice(0, pipe)).trim()
  return target !== ""
}
