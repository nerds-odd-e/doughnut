import YAML from "yaml"

export type ParseNoteContentFailureReason =
  | "malformed_frontmatter_fence"
  | "malformed_yaml"
  | "duplicate_keys"
  | "unsupported_value"

export type ParseNoteContentMarkdownResult =
  | { ok: true; properties: Record<string, string>; body: string }
  | {
      ok: false
      reason: ParseNoteContentFailureReason
      message: string
    }

function stripBom(s: string): string {
  return s.charCodeAt(0) === 0xfeff ? s.slice(1) : s
}

function splitLeadingFrontmatter(
  markdown: string
):
  | { kind: "none" }
  | { kind: "parsed"; yamlRaw: string; body: string; verbatimPrefix: string }
  | { kind: "invalid"; message: string } {
  const text = stripBom(markdown)
  const lines = text.split(/\r?\n/)

  if (lines[0] !== "---") {
    return { kind: "none" }
  }

  for (let i = 1; i < lines.length; i++) {
    if (lines[i] === "---") {
      const yamlRaw = lines.slice(1, i).join("\n")
      const body = lines.slice(i + 1).join("\n")
      const verbatimPrefix = `---\n${yamlRaw}\n---\n`
      return { kind: "parsed", yamlRaw, body, verbatimPrefix }
    }
  }

  return {
    kind: "invalid",
    message:
      "Opening --- was found without a closing --- line for YAML frontmatter.",
  }
}

/** Leading `---` … `---` block including fences, or null when absent or malformed. */
export function verbatimFrontmatterPrefixAndBody(
  markdown: string
): { prefix: string; body: string } | null {
  const split = splitLeadingFrontmatter(markdown)
  if (split.kind !== "parsed") return null
  return { prefix: split.verbatimPrefix, body: split.body }
}

function yamlScalarToPropertyString(value: unknown): string | null {
  if (value === null || value === undefined) return null
  if (typeof value === "string") return value
  if (typeof value === "number" || typeof value === "boolean") {
    return String(value)
  }
  if (typeof value === "bigint") return String(value)
  return null
}

function mappingToProperties(
  map: Record<string, unknown>
):
  | { ok: true; properties: Record<string, string> }
  | Extract<ParseNoteContentMarkdownResult, { ok: false }> {
  const properties: Record<string, string> = {}
  for (const key of Object.keys(map)) {
    const value = yamlScalarToPropertyString(map[key])
    if (value === null) {
      return {
        ok: false,
        reason: "unsupported_value",
        message:
          "Note frontmatter must contain only string, number, or boolean values.",
      }
    }
    properties[key] = value
  }
  return { ok: true, properties }
}

/** Parses leading YAML frontmatter from persisted note Markdown content. */
export function parseNoteContentMarkdown(
  markdown: string
): ParseNoteContentMarkdownResult {
  const split = splitLeadingFrontmatter(markdown)
  if (split.kind === "none") {
    return { ok: true, properties: {}, body: markdown }
  }
  if (split.kind === "invalid") {
    return {
      ok: false,
      reason: "malformed_frontmatter_fence",
      message: split.message,
    }
  }

  const { yamlRaw, body } = split

  if (/^\s*$/.test(yamlRaw)) {
    return { ok: true, properties: {}, body }
  }

  const doc = YAML.parseDocument(yamlRaw, { uniqueKeys: true })
  if (doc.errors.length > 0) {
    const dup = doc.errors.find((e) => e.code === "DUPLICATE_KEY")
    if (dup) {
      return {
        ok: false,
        reason: "duplicate_keys",
        message: dup.message,
      }
    }
    const firstErr = doc.errors[0]
    return {
      ok: false,
      reason: "malformed_yaml",
      message: firstErr?.message ?? "Invalid YAML in note frontmatter.",
    }
  }

  const root = doc.toJS()
  if (root === null || root === undefined) {
    return {
      ok: false,
      reason: "unsupported_value",
      message:
        "Note frontmatter must be a YAML mapping, not a scalar or empty.",
    }
  }
  if (typeof root !== "object" || Array.isArray(root)) {
    return {
      ok: false,
      reason: "unsupported_value",
      message: "Note frontmatter must be a flat YAML mapping.",
    }
  }

  const mapped = mappingToProperties(root as Record<string, unknown>)
  if (!mapped.ok) return mapped

  return { ok: true, properties: mapped.properties, body }
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

/** One key/value row for rich property editing (order matches sorted keys). */
export type PropertyRow = { key: string; value: string }

/** Preset property keys offered in rich-mode property name UI. */
export const RICH_MODE_PRESET_PROPERTY_KEYS = [
  "image",
  "wikidata_id",
  "url",
] as const

/** Rich-mode property key for Wikidata Q-id (YAML may use `wikidata_id` or `wikidataId`). */
export function isWikidataIdPropertyKey(key: string): boolean {
  const t = key.trim().toLowerCase()
  return t === "wikidata_id" || t === "wikidataid"
}

/** Maps parsed scalar properties into sorted rows for stable UI state. */
export function sortedPropertyRowsFromRecord(
  properties: Record<string, string>
): PropertyRow[] {
  const keys = Object.keys(properties)
  if (keys.length === 0) return []
  return keys
    .sort((a, b) => a.localeCompare(b))
    .map((key) => ({ key, value: properties[key]! }))
}

/** Composes content from ordered rows; duplicate keys keep the last occurrence. */
export function composeNoteContentFromPropertyRows(
  rows: readonly PropertyRow[],
  body: string
): string {
  const properties: Record<string, string> = {}
  for (const row of rows) {
    properties[row.key] = row.value
  }
  return composeNoteContentMarkdown({ properties, body })
}

/** Validates rich property rows before persisting or emitting updates (trimmed keys). */
export function validatePropertyRowsForRichEdit(
  rows: readonly PropertyRow[]
): { ok: true } | { ok: false; message: string } {
  const trimmed = rows.map((r) => ({
    key: r.key.trim(),
    value: r.value.trim(),
  }))
  let emptyKeyCount = 0
  for (const r of trimmed) {
    if (!r.key) {
      emptyKeyCount++
      if (!r.value) {
        return {
          ok: false,
          message:
            "A property with an empty key must have a value until you name the key.",
        }
      }
    }
  }
  if (emptyKeyCount > 1) {
    return {
      ok: false,
      message: "Only one property may have an empty key at a time.",
    }
  }
  const keys = trimmed.map((r) => r.key)
  const seen = new Set<string>()
  for (const k of keys) {
    if (seen.has(k)) {
      return { ok: false, message: "Duplicate property keys are not allowed." }
    }
    seen.add(k)
  }
  return { ok: true }
}

export function insertPropertyRowAt(
  rows: readonly PropertyRow[],
  index: number,
  row: PropertyRow
): PropertyRow[] {
  const next = [...rows]
  next.splice(index, 0, row)
  return next
}

export function renamePropertyRowKeyAt(
  rows: readonly PropertyRow[],
  index: number,
  newKey: string
): PropertyRow[] {
  return rows.map((r, i) => (i === index ? { ...r, key: newKey } : r))
}

export function removePropertyRowAt(
  rows: readonly PropertyRow[],
  index: number
): PropertyRow[] {
  return rows.filter((_, i) => i !== index)
}
