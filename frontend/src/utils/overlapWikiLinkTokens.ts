import {
  findStringListPropertyKey,
  normalizedLookupKey,
} from "@/utils/frontmatterStringList"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatter"
import {
  isListPropertyValue,
  type NoteProperties,
} from "@/utils/noteProperties"
import { isWellFormedWholeWikiLinkItem } from "@/utils/authoredLinkMarkup"

function overlapWikiLinkTokensFromProperties(
  properties: NoteProperties
): string[] {
  const existingKey = findStringListPropertyKey(properties, "overlaps")
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
 * Tokens used for overlap declaration checks: authored `overlaps` only.
 * Wiki-link items under `aliases` do not contribute. Mirrors backend
 * `FrontmatterOverlaps.overlapWikiLinkTokensFromNoteContent`.
 */
export function overlapWikiLinkTokensFromNoteContent(
  contentMarkdown: string
): string[] {
  const parsed = parseNoteContentMarkdown(contentMarkdown)
  if (!parsed.ok) return []
  return overlapWikiLinkTokensFromProperties(parsed.properties)
}

/** True when `wikiLinkToken` is already among authored `overlaps` tokens. */
export function noteContentDeclaresOverlapWikiLink(
  contentMarkdown: string,
  wikiLinkToken: string
): boolean {
  const trimmed = wikiLinkToken.trim()
  if (!trimmed) return false
  const targetKey = normalizedLookupKey(trimmed)
  return overlapWikiLinkTokensFromNoteContent(contentMarkdown).some(
    (existing) => normalizedLookupKey(existing) === targetKey
  )
}
