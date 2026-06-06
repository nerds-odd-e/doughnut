import YAML from "yaml"
import { isTitlePatternPropertyKey } from "@/utils/noteContentPropertyKeys"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatterParse"

export {
  INDEX_ONLY_PRESET_PROPERTY_KEYS,
  RICH_MODE_PRESET_PROPERTY_KEYS,
  isExampleOfPropertyKey,
  isImagePropertyKey,
  isQuestionGenerationInstructionPropertyKey,
  isReservedIndexOnlyPropertyKey,
  isTitlePatternPropertyKey,
  isUrlPropertyKey,
  isWikidataIdPropertyKey,
  nextAvailablePropertyKeyForPreset,
  propertyKeyBaseAndSuffix,
  propertyKeyMatchesPresetFamily,
  richModeKeyDropdownPresetKeys,
  richModeKeyDropdownPresetKeysForPropertyRows,
  rowFillsIndexOnlyPresetSlot,
} from "@/utils/noteContentPropertyKeys"

export {
  type PropertyRow,
  composeNoteContentFromPropertyRows,
  insertPropertyRowAt,
  removePropertyRowAt,
  renamePropertyRowKeyAt,
  sortedPropertyRowsFromRecord,
  validatePropertyRowsForRichEdit,
} from "@/utils/noteContentPropertyRows"

export {
  type ParseNoteContentFailureReason,
  type ParseNoteContentMarkdownResult,
  firstScalarValueFromYamlBlock,
  noteImageScalarsFromMarkdown,
  parseNoteContentMarkdown,
  verbatimFrontmatterPrefixAndBody,
} from "@/utils/noteContentFrontmatterParse"

/** Reads scoped title pattern from leading YAML (`title_pattern` or legacy `titlePattern`; key match ignores case and underscores). */
export function titlePatternFromNoteMarkdown(
  markdown: string | undefined | null
): string | undefined {
  if (markdown == null || markdown === "") return
  const p = parseNoteContentMarkdown(markdown)
  if (!p.ok) return
  for (const [k, v] of Object.entries(p.properties)) {
    if (isTitlePatternPropertyKey(k)) {
      const t = (v ?? "").trim()
      return t.length > 0 ? t : undefined
    }
  }
  return
}

/** True when any key is the `relation` property (trimmed, case-insensitive), matching rich editor rows. */
export function propertyRecordHasRelationKey(
  properties: Record<string, string>
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
  properties: Record<string, string>
  body: string
}): string {
  if (Object.keys(input.properties).length === 0) {
    return input.body
  }

  const yamlBlock = YAML.stringify(input.properties, {
    lineWidth: 0,
  }).trimEnd()

  return `---\n${yamlBlock}\n---\n${input.body}`
}

export function isRelationPropertyKey(key: string): boolean {
  return key.trim().toLowerCase() === "relation"
}
