/** NFKC + lower-case key for alias dedupe; matches backend `FrontmatterAliases.normalizedLookupKey`. */
export function normalizedLookupKey(alias: string): string {
  return alias.normalize("NFKC").toLowerCase()
}

/** Appends `alias` when its normalized key is new; otherwise returns null (no change). */
export function mergeAliasIntoList(
  existingItems: readonly string[],
  alias: string
): string[] | null {
  const trimmedAlias = alias.trim()
  if (!trimmedAlias) return null

  const newKey = normalizedLookupKey(trimmedAlias)
  const alreadyPresent = existingItems.some(
    (item) => normalizedLookupKey(item.trim()) === newKey
  )
  if (alreadyPresent) return null

  return [...existingItems, trimmedAlias]
}
