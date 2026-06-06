import type { PropertyRow } from "@/utils/noteContentPropertyRows"

/** Preset property keys offered in rich-mode property name UI. */
export const RICH_MODE_PRESET_PROPERTY_KEYS = [
  "image",
  "wikidata_id",
  "url",
  "example of",
] as const

/**
 * Predefined property keys shown only when editing a designated index note
 * (notebook index, folder index, or direct /n<noteId> for those notes).
 */
export const INDEX_ONLY_PRESET_PROPERTY_KEYS = [
  "title_pattern",
  "question_generation_instruction",
] as const

/** Splits a property key into its base name and optional numeric suffix (`url 2` → suffix 2). */
export function propertyKeyBaseAndSuffix(key: string): {
  base: string
  suffix: number | null
} {
  const trimmed = key.trim()
  const match = trimmed.match(/^(.+?) (\d+)$/)
  if (match) {
    const n = Number.parseInt(match[2]!, 10)
    if (n >= 2) {
      return { base: match[1]!, suffix: n }
    }
  }
  return { base: trimmed, suffix: null }
}

function propertyKeyBaseMatches(
  key: string,
  bases: string | readonly string[]
): boolean {
  const { base } = propertyKeyBaseAndSuffix(key)
  const normalized = base.trim().toLowerCase()
  const list = typeof bases === "string" ? [bases] : bases
  return list.some((b) => b === normalized)
}

/** Rich-mode property key for Wikidata Q-id (YAML may use `wikidata_id` or `wikidataId`). */
export function isWikidataIdPropertyKey(key: string): boolean {
  return propertyKeyBaseMatches(key, ["wikidata_id", "wikidataid"])
}

/**
 * Rich-mode property key for header image upload (`image:` only).
 * `image_mask` and other keys use the normal text/value editor.
 */
export function isImagePropertyKey(key: string): boolean {
  return propertyKeyBaseMatches(key, "image")
}

export function isUrlPropertyKey(key: string): boolean {
  return propertyKeyBaseMatches(key, "url")
}

export function isExampleOfPropertyKey(key: string): boolean {
  return propertyKeyBaseMatches(key, "example of")
}

/** True when `key` is the title-pattern slot (`title_pattern`, `titlePattern`, etc.). */
export function isTitlePatternPropertyKey(key: string): boolean {
  const { base } = propertyKeyBaseAndSuffix(key)
  const t = base.trim().toLowerCase().replace(/_/g, "")
  return t === "titlepattern"
}

/** True when `key` is the question-generation instruction slot (canonical or legacy camelCase). */
export function isQuestionGenerationInstructionPropertyKey(
  key: string
): boolean {
  const { base } = propertyKeyBaseAndSuffix(key)
  const t = base.trim().toLowerCase().replace(/_/g, "")
  return t === "questiongenerationinstruction"
}

/** True when `key` is any index-only predefined slot, including legacy camelCase aliases. */
export function isReservedIndexOnlyPropertyKey(key: string): boolean {
  return (
    (INDEX_ONLY_PRESET_PROPERTY_KEYS as readonly string[]).includes(key) ||
    isTitlePatternPropertyKey(key) ||
    isQuestionGenerationInstructionPropertyKey(key)
  )
}

/** True when `rowKey` already fills the slot for `canonicalPresetKey` (same key or legacy alias). */
export function rowFillsIndexOnlyPresetSlot(
  rowKey: string,
  canonicalPresetKey: (typeof INDEX_ONLY_PRESET_PROPERTY_KEYS)[number]
): boolean {
  if (rowKey === canonicalPresetKey) return true
  if (canonicalPresetKey === "title_pattern") {
    return isTitlePatternPropertyKey(rowKey)
  }
  if (canonicalPresetKey === "question_generation_instruction") {
    return isQuestionGenerationInstructionPropertyKey(rowKey)
  }
  return false
}

/** True when `key` belongs to the preset family named by `presetKey`. */
export function propertyKeyMatchesPresetFamily(
  key: string,
  presetKey: string
): boolean {
  switch (presetKey) {
    case "image":
      return isImagePropertyKey(key)
    case "wikidata_id":
      return isWikidataIdPropertyKey(key)
    case "url":
      return isUrlPropertyKey(key)
    case "example of":
      return isExampleOfPropertyKey(key)
    case "title_pattern":
      return rowFillsIndexOnlyPresetSlot(key, "title_pattern")
    case "question_generation_instruction":
      return rowFillsIndexOnlyPresetSlot(key, "question_generation_instruction")
    default:
      return false
  }
}

function propertyKeyBaseMatchesBaseKey(key: string, baseKey: string): boolean {
  const { base } = propertyKeyBaseAndSuffix(key)
  return base.trim().toLowerCase() === baseKey.trim().toLowerCase()
}

function nextAvailablePropertyKeyFromFamilyKeys(
  baseKey: string,
  familyKeys: readonly string[]
): string {
  const occupied = new Set<number>()
  for (const k of familyKeys) {
    const trimmed = k.trim()
    if (!trimmed) continue
    const { suffix } = propertyKeyBaseAndSuffix(trimmed)
    occupied.add(suffix ?? 1)
  }
  if (!occupied.has(1)) return baseKey
  let n = 2
  while (occupied.has(n)) n++
  return `${baseKey} ${n}`
}

/** Next free key in a base-key family, using `key 2`, `key 3`, … when the base is taken. */
export function nextAvailablePropertyKeyForBase(
  baseKey: string,
  existingKeys: readonly string[]
): string {
  const familyKeys = existingKeys.filter((k) =>
    propertyKeyBaseMatchesBaseKey(k, baseKey)
  )
  return nextAvailablePropertyKeyFromFamilyKeys(baseKey, familyKeys)
}

/** Next free key for a preset family, using `key 2`, `key 3`, … when the base is taken. */
export function nextAvailablePropertyKeyForPreset(
  presetKey: string,
  rows: readonly PropertyRow[],
  options?: { excludeRowIndex?: number }
): string {
  const familyKeys: string[] = []
  for (let i = 0; i < rows.length; i++) {
    if (options?.excludeRowIndex === i) continue
    const k = rows[i]!.key.trim()
    if (!k) continue
    if (!propertyKeyMatchesPresetFamily(k, presetKey)) continue
    familyKeys.push(k)
  }
  return nextAvailablePropertyKeyFromFamilyKeys(presetKey, familyKeys)
}

/** Keys offered in the rich-mode property key dropdown (insert and row key fields). */
export function richModeKeyDropdownPresetKeys(
  isIndexContext: boolean
): string[] {
  const keys: string[] = [...RICH_MODE_PRESET_PROPERTY_KEYS]
  if (isIndexContext) keys.push(...INDEX_ONLY_PRESET_PROPERTY_KEYS)
  return keys
}

/**
 * Preset keys for the rich-mode property key dropdown, each resolved to the next
 * available name in its family (e.g. `url 2` when `url` already exists).
 */
export function richModeKeyDropdownPresetKeysForPropertyRows(
  isIndexContext: boolean,
  rows: readonly PropertyRow[],
  options?: { excludeRowIndex?: number }
): string[] {
  return richModeKeyDropdownPresetKeys(isIndexContext).map((preset) =>
    nextAvailablePropertyKeyForPreset(preset, rows, options)
  )
}
