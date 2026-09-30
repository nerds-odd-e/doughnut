import YAML from "yaml"
import { isTitlePatternPropertyKey } from "@/utils/noteContentPropertyKeys"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatterParse"
import {
  type NoteProperties,
  type PropertyValue,
  scalarStringFromPropertyValue,
  yamlRecordFromNoteProperties,
} from "@/utils/noteProperties"

export {
  README_ONLY_PRESET_PROPERTY_KEYS,
  findPropertyRowIndexByExactKey,
  isExampleOfPropertyKey,
  isImagePropertyKey,
  isListCapablePropertyKey,
  isNoteLevelPropertyKey,
  isQuestionGenerationInstructionPropertyKey,
  isReservedReadmeOnlyPropertyKey,
  isScalarOnlyStructuralPropertyKey,
  isTextCapablePropertyRow,
  isTitlePatternPropertyKey,
  isUrlPropertyKey,
  isWikidataIdPropertyKey,
  nextAvailablePropertyKeyForPreset,
  propertyKeyBaseAndSuffix,
  propertyKeyMatchesPresetFamily,
  rowFillsReadmeOnlyPresetSlot,
} from "@/utils/noteContentPropertyKeys"
export {
  NOTE_ONLY_PRESET_PROPERTY_KEYS,
  RICH_MODE_PRESET_PROPERTY_KEYS,
  richModeKeyDropdownPresetKeys,
  richModeKeyDropdownPresetKeysForPropertyRows,
} from "@/utils/noteContentPropertyKeyPresets"

export {
  type PropertyRow,
  appendValueToPropertyRow,
  appendWikiLinkPropertyRow,
  composeNoteContentFromPropertyRows,
  insertPropertyRowAt,
  normalizePropertyRowForCommit,
  notePropertiesFromPropertyRows,
  propertyRowForInsertedKey,
  propertyRowWithScalar,
  propertyRowsAfterAppendingValueToExactKey,
  removePropertyRowAt,
  renamePropertyRowKeyAt,
  scalarStringFromPropertyRow,
  propertyRowsFromNoteProperties,
  validatePropertyRowsForRichEdit,
} from "@/utils/noteContentPropertyRows"
export { composeNoteContentInPlace } from "@/utils/noteContentInPlaceEdit"

export {
  type NoteProperties,
  type ParseNoteContentFailureReason,
  type ParseNoteContentMarkdownResult,
  type PropertyValue,
  firstScalarValueFromYamlBlock,
  noteImageScalarsFromMarkdown,
  parseNoteContentMarkdown,
  verbatimFrontmatterPrefixAndBody,
} from "@/utils/noteContentFrontmatterParse"

export {
  frontmatterScalar,
  compactDisplayForPropertyValue,
  isListPropertyValue,
  isScalarPropertyValue,
  listPropertyValue,
  notePropertiesFromScalarRecord,
  propertyValueHasContent,
  scalarPropertyValue,
  scalarRecordFromNoteProperties,
  scalarStringFromPropertyValue,
  yamlRecordFromNoteProperties,
  yamlValueToPropertyValue,
} from "@/utils/noteProperties"

/** Reads scoped title pattern from leading YAML (`title_pattern` or legacy `titlePattern`; key match ignores case and underscores). */
export function titlePatternFromNoteMarkdown(
  markdown: string | undefined | null
): string | undefined {
  if (markdown == null || markdown === "") return
  const p = parseNoteContentMarkdown(markdown)
  if (!p.ok) return
  for (const [k, v] of Object.entries(p.properties)) {
    if (isTitlePatternPropertyKey(k)) {
      const t = (scalarStringFromPropertyValue(v) ?? "").trim()
      return t.length > 0 ? t : undefined
    }
  }
  return
}

/** True when any key is the `relation` property (trimmed, case-insensitive), matching rich editor rows. */
export function propertyRecordHasRelationKey(
  properties: Record<string, string> | NoteProperties
): boolean {
  for (const key of Object.keys(properties)) {
    if (key.trim().toLowerCase() === "relation") return true
  }
  return false
}

/** True when parsed content includes a `relation` frontmatter key. */
export function contentHasRelationProperty(markdown: string): boolean {
  const p = parseNoteContentMarkdown(markdown)
  if (!p.ok) return false
  return propertyRecordHasRelationKey(p.properties)
}

/** Composes note Markdown content with optional leading YAML frontmatter. */
export function composeNoteContentMarkdown(input: {
  properties: NoteProperties
  body: string
}): string {
  if (Object.keys(input.properties).length === 0) {
    return input.body
  }

  const yamlBlock = YAML.stringify(
    yamlRecordFromNoteProperties(input.properties),
    {
      lineWidth: 0,
    }
  ).trimEnd()

  return `---\n${yamlBlock}\n---\n${input.body}`
}

export function isRelationPropertyKey(key: string): boolean {
  return key.trim().toLowerCase() === "relation"
}

export type PropertyKeyChange =
  | { type: "removal"; key: string }
  | { type: "rename"; fromKey: string; toKey: string }

function propertyValueIdentity(value: PropertyValue): string {
  return value.kind === "scalar"
    ? value.value.trim()
    : JSON.stringify(value.items)
}

export type PropertyValueFollow = { key: string; value: string }

/** Keys whose single value became one value of a list between two note Markdown snapshots. */
export function diffSingleValuesBecomingListValues(
  oldMarkdown: string,
  newMarkdown: string
): PropertyValueFollow[] {
  const oldParsed = parseNoteContentMarkdown(oldMarkdown)
  const newParsed = parseNoteContentMarkdown(newMarkdown)
  if (!oldParsed.ok || !newParsed.ok) return []

  return Object.entries(oldParsed.properties).flatMap(([key, oldValue]) => {
    const newValue = newParsed.properties[key]
    if (oldValue.kind !== "scalar" || newValue?.kind !== "list") return []
    const value = oldValue.value.trim()
    return newValue.items.includes(value) ? [{ key, value }] : []
  })
}

/** Detects property key removals and renames between two note Markdown snapshots. */
export function diffFrontmatterPropertyKeyChanges(
  oldMarkdown: string,
  newMarkdown: string
): PropertyKeyChange[] {
  const oldParsed = parseNoteContentMarkdown(oldMarkdown)
  const newParsed = parseNoteContentMarkdown(newMarkdown)
  if (!oldParsed.ok || !newParsed.ok) return []

  const oldProps = oldParsed.properties
  const newProps = newParsed.properties

  const removedKeys: string[] = []
  for (const key of Object.keys(oldProps)) {
    if (!(key in newProps)) removedKeys.push(key)
  }

  const addedKeys: string[] = []
  for (const key of Object.keys(newProps)) {
    if (!(key in oldProps)) addedKeys.push(key)
  }

  const removedByValue = new Map<string, string[]>()
  for (const key of removedKeys) {
    const value = propertyValueIdentity(oldProps[key]!)
    const list = removedByValue.get(value) ?? []
    list.push(key)
    removedByValue.set(value, list)
  }

  const addedByValue = new Map<string, string[]>()
  for (const key of addedKeys) {
    const value = propertyValueIdentity(newProps[key]!)
    const list = addedByValue.get(value) ?? []
    list.push(key)
    addedByValue.set(value, list)
  }

  const renames: PropertyKeyChange[] = []
  const pairedRemovedKeys = new Set<string>()

  for (const [value, removedList] of removedByValue) {
    const addedList = addedByValue.get(value)
    if (addedList && removedList.length === 1 && addedList.length === 1) {
      renames.push({
        type: "rename",
        fromKey: removedList[0]!,
        toKey: addedList[0]!,
      })
      pairedRemovedKeys.add(removedList[0]!)
    }
  }

  const removals: PropertyKeyChange[] = []
  for (const key of removedKeys) {
    if (!pairedRemovedKeys.has(key)) {
      removals.push({ type: "removal", key })
    }
  }

  return [...renames, ...removals]
}
