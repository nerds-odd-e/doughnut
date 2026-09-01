import type { WikiLink } from "@generated/donut-backend-api"
import {
  noteIdForAuthoredToken,
  parseWholeWikiLinkItem,
  isWellFormedWholeWikiLinkItem,
} from "@/utils/authoredLinkMarkup"
import {
  findStringListPropertyKey,
  normalizedLookupKey,
} from "@/utils/frontmatterStringList"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatter"
import {
  isListPropertyValue,
  type NoteProperties,
} from "@/utils/noteProperties"
import { wikiLinkNoteIdLookup } from "@/utils/wikiLinkMarkup"

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
function overlapWikiLinkTokensFromNoteContent(
  contentMarkdown: string
): string[] {
  const parsed = parseNoteContentMarkdown(contentMarkdown)
  if (!parsed.ok) return []
  return overlapWikiLinkTokensFromProperties(parsed.properties)
}

/**
 * True when authored `overlaps` already names `destinationNoteId` via a
 * resolved wiki link — compared by destination id, not reconstructed spelling.
 */
export function noteContentDeclaresOverlapToDestination(
  contentMarkdown: string,
  wikiLinks: readonly WikiLink[],
  destinationNoteId: number
): boolean {
  const lookup = wikiLinkNoteIdLookup(wikiLinks)
  return overlapWikiLinkTokensFromNoteContent(contentMarkdown).some((token) => {
    const parsed = parseWholeWikiLinkItem(token)
    if (!parsed) return false
    return noteIdForAuthoredToken(parsed.inner, lookup) === destinationNoteId
  })
}
