import type { PropertyRow } from "@/utils/noteContentPropertyRows"
import {
  README_ONLY_PRESET_PROPERTY_KEYS,
  keysInPresetFamily,
  nextAvailablePropertyKeyForPreset,
} from "@/utils/noteContentPropertyKeys"

/**
 * Preset keys for ordinary notes only (not folder/notebook readme).
 * Each is a singleton, so an occupied key is omitted rather than offered as a
 * suffixed key (`aliases 2`, `note_level 2`, …).
 */
export const NOTE_ONLY_PRESET_PROPERTY_KEYS = [
  "aliases",
  "overlaps",
  "note_level",
] as const

/** Preset property keys offered in rich-mode property name UI. */
export const RICH_MODE_PRESET_PROPERTY_KEYS = [
  "image",
  "wikidata_id",
  "url",
  "example of",
  "question_generation_instruction",
] as const

function isNoteOnlyPresetPropertyKey(key: string): boolean {
  return (NOTE_ONLY_PRESET_PROPERTY_KEYS as readonly string[]).includes(key)
}

/** Keys offered in the rich-mode property key dropdown (insert and row key fields). */
export function richModeKeyDropdownPresetKeys(
  isReadmeContext: boolean
): string[] {
  if (isReadmeContext) {
    return [
      ...RICH_MODE_PRESET_PROPERTY_KEYS,
      ...README_ONLY_PRESET_PROPERTY_KEYS,
    ]
  }
  return [...NOTE_ONLY_PRESET_PROPERTY_KEYS, ...RICH_MODE_PRESET_PROPERTY_KEYS]
}

/**
 * Preset keys for the rich-mode property key dropdown, each resolved to the next
 * available name in its family (e.g. `url 2` when `url` already exists).
 * Occupied note-only singleton keys are omitted instead of suffixed.
 */
export function richModeKeyDropdownPresetKeysForPropertyRows(
  isReadmeContext: boolean,
  rows: readonly PropertyRow[],
  options?: { excludeRowIndex?: number }
): string[] {
  return richModeKeyDropdownPresetKeys(isReadmeContext).flatMap((preset) => {
    if (isNoteOnlyPresetPropertyKey(preset)) {
      if (keysInPresetFamily(preset, rows, options).length > 0) return []
      return [preset]
    }
    return [nextAvailablePropertyKeyForPreset(preset, rows, options)]
  })
}
