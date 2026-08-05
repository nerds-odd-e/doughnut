import {
  findStringListPropertyKey,
  normalizedLookupKey,
} from "@/utils/frontmatterStringList"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatter"
import {
  isListPropertyValue,
  type NoteProperties,
} from "@/utils/noteProperties"
import { isWellFormedWholeWikiLinkItem } from "@/utils/wholeWikiLinkItem"

function wikiLinkTokensFromListKey(
  properties: NoteProperties,
  listKey: string
): string[] {
  const existingKey = findStringListPropertyKey(properties, listKey)
  if (!existingKey) return []
  const value = properties[existingKey]
  if (value === undefined || !isListPropertyValue(value)) return []

  const tokens: string[] = []
  const seen = new Set<string>()
  for (const item of value.items) {
    const trimmed = item.trim()
    if (!trimmed || !isWellFormedWholeWikiLinkItem(trimmed)) continue
    const key = normalizedLookupKey(trimmed)
    if (seen.has(key)) continue
    seen.add(key)
    tokens.push(trimmed)
  }
  return tokens
}

/**
 * Tokens used for overlap declaration checks: authored `overlaps` plus legacy
 * wiki-link items in `aliases` (union, overlaps first, normalized dedupe).
 * Mirrors backend `FrontmatterOverlaps.gradingOverlapWikiLinkTokensFromNoteContent`.
 */
export function gradingOverlapWikiLinkTokensFromNoteContent(
  contentMarkdown: string
): string[] {
  const parsed = parseNoteContentMarkdown(contentMarkdown)
  if (!parsed.ok) return []

  const fromOverlaps = wikiLinkTokensFromListKey(parsed.properties, "overlaps")
  const fromAliases = wikiLinkTokensFromListKey(parsed.properties, "aliases")
  const seen = new Set(fromOverlaps.map(normalizedLookupKey))
  const out = [...fromOverlaps]
  for (const token of fromAliases) {
    const key = normalizedLookupKey(token)
    if (seen.has(key)) continue
    seen.add(key)
    out.push(token)
  }
  return out
}

/** True when `wikiLinkToken` is already among grading overlap tokens. */
export function noteContentDeclaresOverlapWikiLink(
  contentMarkdown: string,
  wikiLinkToken: string
): boolean {
  const trimmed = wikiLinkToken.trim()
  if (!trimmed) return false
  const targetKey = normalizedLookupKey(trimmed)
  return gradingOverlapWikiLinkTokensFromNoteContent(contentMarkdown).some(
    (existing) => normalizedLookupKey(existing) === targetKey
  )
}
