import { composeNoteContentMarkdown } from "@/utils/noteContentFrontmatter"
import { findPropertyRowIndexByExactKey } from "@/utils/noteContentPropertyKeys"
import {
  authoredAliasesValidationErrorForPropertyRow,
  isAliasesPropertyKey,
} from "@/utils/authoredAliasesValidation"
import {
  type NoteProperties,
  type PropertyValue,
  listPropertyValue,
  notePropertiesFromScalarRecord,
  propertyValueHasContent,
  scalarPropertyValue,
  scalarStringFromPropertyValue,
} from "@/utils/noteProperties"

export type PropertyRow = { key: string; value: PropertyValue }

export function propertyRowWithScalar(key: string, value: string): PropertyRow {
  return { key, value: scalarPropertyValue(value) }
}

export function propertyRowForInsertedKey(
  key: string,
  value: string
): PropertyRow {
  if (isAliasesPropertyKey(key)) {
    return { key, value: listPropertyValue([value.trim()]) }
  }
  return propertyRowWithScalar(key, value)
}

/** Builds note properties from rich-editor property rows (last row wins on duplicate keys). */
export function notePropertiesFromPropertyRows(
  rows: readonly PropertyRow[]
): NoteProperties {
  const properties: NoteProperties = {}
  for (const row of rows) {
    properties[row.key] = row.value
  }
  return properties
}

/** Maps parsed properties into sorted rows for stable UI state. */
export function sortedPropertyRowsFromNoteProperties(
  properties: NoteProperties
): PropertyRow[] {
  const keys = Object.keys(properties)
  if (keys.length === 0) return []
  return keys
    .sort((a, b) => a.localeCompare(b))
    .map((key) => ({ key, value: properties[key]! }))
}

/** Maps legacy scalar property records into sorted rows for stable UI state. */
export function sortedPropertyRowsFromRecord(
  properties: Record<string, string>
): PropertyRow[] {
  return sortedPropertyRowsFromNoteProperties(
    notePropertiesFromScalarRecord(properties)
  )
}

/** Composes content from ordered rows; duplicate keys keep the last occurrence. */
export function composeNoteContentFromPropertyRows(
  rows: readonly PropertyRow[],
  body: string
): string {
  return composeNoteContentMarkdown({
    properties: notePropertiesFromPropertyRows(rows),
    body,
  })
}

/** Trims scalar row values; list values are preserved as-is. */
export function normalizePropertyRowForCommit(row: PropertyRow): PropertyRow {
  return {
    key: row.key.trim(),
    value:
      row.value.kind === "scalar"
        ? scalarPropertyValue(row.value.value.trim())
        : row.value,
  }
}

/** Validates rich property rows before persisting or emitting updates (trimmed keys). */
export function validatePropertyRowsForRichEdit(
  rows: readonly PropertyRow[]
): { ok: true } | { ok: false; message: string } {
  const trimmed = rows.map(normalizePropertyRowForCommit)
  let emptyKeyCount = 0
  for (const r of trimmed) {
    if (!r.key) {
      emptyKeyCount++
      if (!propertyValueHasContent(r.value)) {
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
  for (const row of trimmed) {
    const aliasError = authoredAliasesValidationErrorForPropertyRow(row)
    if (aliasError) {
      return { ok: false, message: aliasError }
    }
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

/** Scalar string for a row when the value is scalar; undefined for lists. */
export function scalarStringFromPropertyRow(
  row: PropertyRow
): string | undefined {
  return scalarStringFromPropertyValue(row.value)
}

/** Appends a value to a row, promoting scalars to a two-item list when needed. */
export function appendValueToPropertyRow(
  row: PropertyRow,
  value: string
): PropertyRow {
  const trimmed = value.trim()
  if (row.value.kind === "list") {
    return {
      key: row.key,
      value: listPropertyValue([...row.value.items, trimmed]),
    }
  }
  const existing = scalarStringFromPropertyValue(row.value) ?? ""
  const items = propertyValueHasContent(row.value)
    ? [existing, trimmed]
    : [trimmed]
  return {
    key: row.key,
    value: listPropertyValue(items),
  }
}

/** Returns rows with `value` appended to the exact `key` row, or null when absent. */
export function propertyRowsAfterAppendingValueToExactKey(
  rows: readonly PropertyRow[],
  key: string,
  value: string
): PropertyRow[] | null {
  const idx = findPropertyRowIndexByExactKey(rows, key)
  if (idx < 0) return null
  return rows.map((r, i) =>
    i === idx ? appendValueToPropertyRow(r, value) : r
  )
}
