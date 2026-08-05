import {
  composeNoteContentMarkdown,
  parseNoteContentMarkdown,
} from "./noteContentFrontmatter"
import {
  isListPropertyValue,
  listPropertyValue,
  type NoteProperties,
} from "./noteProperties"

/** NFKC + lower-case key for string-list dedupe; matches backend `FrontmatterAliases.normalizedLookupKey`. */
export function normalizedLookupKey(item: string): string {
  return item.normalize("NFKC").toLowerCase()
}

/** Appends `item` when its normalized key is new; otherwise returns null (no change). */
export function mergeItemIntoStringList(
  existingItems: readonly string[],
  item: string
): string[] | null {
  const trimmed = item.trim()
  if (!trimmed) return null

  const newKey = normalizedLookupKey(trimmed)
  const alreadyPresent = existingItems.some(
    (existing) => normalizedLookupKey(existing.trim()) === newKey
  )
  if (alreadyPresent) return null

  return [...existingItems, trimmed]
}

/** Existing property key matching `listKey` case-insensitively, or undefined. */
export function findStringListPropertyKey(
  properties: NoteProperties,
  listKey: string
): string | undefined {
  const target = listKey.trim().toLowerCase()
  for (const key of Object.keys(properties)) {
    if (key.trim().toLowerCase() === target) return key
  }
  return
}

/**
 * Returns updated note markdown with `item` in frontmatter `listKey`, or null when
 * content is unchanged, unparseable, or the key is present but not a YAML list.
 */
export function appendItemToFrontmatterStringList(
  contentMarkdown: string,
  listKey: string,
  item: string
): string | null {
  const trimmedItem = item.trim()
  const canonicalKey = listKey.trim()
  if (!trimmedItem || !canonicalKey) return null

  const parsed = parseNoteContentMarkdown(contentMarkdown)
  if (!parsed.ok) return null

  const existingKey = findStringListPropertyKey(parsed.properties, canonicalKey)
  if (!existingKey) {
    return composeNoteContentMarkdown({
      properties: {
        ...parsed.properties,
        [canonicalKey]: listPropertyValue([trimmedItem]),
      },
      body: parsed.body,
    })
  }

  const existingValue = parsed.properties[existingKey]
  if (existingValue === undefined || !isListPropertyValue(existingValue)) {
    return null
  }

  const merged = mergeItemIntoStringList(existingValue.items, trimmedItem)
  if (merged === null) return null

  return composeNoteContentMarkdown({
    properties: {
      ...parsed.properties,
      [existingKey]: listPropertyValue(merged),
    },
    body: parsed.body,
  })
}
