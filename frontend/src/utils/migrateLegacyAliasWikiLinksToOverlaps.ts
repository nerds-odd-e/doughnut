import {
  findStringListPropertyKey,
  mergeItemIntoStringList,
  normalizedLookupKey,
} from "@/utils/frontmatterStringList"
import {
  composeNoteContentMarkdown,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import {
  isListPropertyValue,
  listPropertyValue,
  type NoteProperties,
} from "@/utils/noteProperties"
import { isWellFormedWholeWikiLinkItem } from "@/utils/wholeWikiLinkItem"

function wikiLinkItemsFromAliasesList(items: readonly string[]): string[] {
  const tokens: string[] = []
  const seen = new Set<string>()
  for (const item of items) {
    const trimmed = item.trim()
    if (!trimmed || !isWellFormedWholeWikiLinkItem(trimmed)) continue
    const key = normalizedLookupKey(trimmed)
    if (seen.has(key)) continue
    seen.add(key)
    tokens.push(trimmed)
  }
  return tokens
}

function plainAliasItems(items: readonly string[]): string[] {
  return items.filter((item) => {
    const trimmed = item.trim()
    return trimmed && !isWellFormedWholeWikiLinkItem(trimmed)
  })
}

/**
 * Moves well-formed wiki-link items from `aliases` into `overlaps` (merge/dedupe).
 * Returns null when unchanged or content is unparseable.
 */
export function migrateLegacyAliasWikiLinksToOverlaps(
  contentMarkdown: string
): string | null {
  const parsed = parseNoteContentMarkdown(contentMarkdown)
  if (!parsed.ok) return null

  const aliasesKey = findStringListPropertyKey(parsed.properties, "aliases")
  if (!aliasesKey) return null
  const aliasesValue = parsed.properties[aliasesKey]
  if (aliasesValue === undefined || !isListPropertyValue(aliasesValue)) {
    return null
  }

  const legacyWiki = wikiLinkItemsFromAliasesList(aliasesValue.items)
  if (legacyWiki.length === 0) return null

  const next: NoteProperties = { ...parsed.properties }
  const plain = plainAliasItems(aliasesValue.items)
  if (plain.length === 0) {
    delete next[aliasesKey]
  } else {
    next[aliasesKey] = listPropertyValue(plain)
  }

  const overlapsKey =
    findStringListPropertyKey(parsed.properties, "overlaps") ?? "overlaps"
  const existingOverlaps = parsed.properties[overlapsKey]
  let overlapItems: string[] =
    existingOverlaps !== undefined && isListPropertyValue(existingOverlaps)
      ? [...existingOverlaps.items]
      : []
  for (const token of legacyWiki) {
    const merged = mergeItemIntoStringList(overlapItems, token)
    if (merged !== null) overlapItems = merged
  }
  next[overlapsKey] = listPropertyValue(overlapItems)

  return composeNoteContentMarkdown({
    properties: next,
    body: parsed.body,
  })
}
