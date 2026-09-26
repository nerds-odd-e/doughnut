import { isEqual } from "es-toolkit"
import { composeNoteContentMarkdown } from "@/utils/noteContentFrontmatter"
import {
  parseNoteContentMarkdown,
  verbatimFrontmatterPrefixAndBody,
} from "@/utils/noteContentFrontmatterParse"
import { findPropertyRowIndexByExactKey } from "@/utils/noteContentPropertyKeys"
import {
  authoredListPropertyValidationErrorForPropertyRow,
  isAuthoredListPropertyKey,
} from "@/utils/authoredListPropertyValidation"
import { authoredNoteLevelValidationErrorForPropertyRow } from "@/utils/authoredNoteLevelValidation"
import {
  type NoteProperties,
  type PropertyValue,
  listPropertyValue,
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
  if (isAuthoredListPropertyKey(key)) {
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

/** Maps parsed properties into rows in their authored order. */
export function propertyRowsFromNoteProperties(
  properties: NoteProperties
): PropertyRow[] {
  return Object.entries(properties).map(([key, value]) => ({ key, value }))
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

/**
 * Composes `body` after the authored frontmatter text and separator of `authored`,
 * keeping that text as written when the property rows leave it unchanged.
 */
export function composeNoteContentInPlace(
  authored: string,
  rows: readonly PropertyRow[],
  body: string
): string {
  const split = verbatimFrontmatterPrefixAndBody(authored)
  const parsed = parseNoteContentMarkdown(authored)
  if (
    split === null ||
    (parsed.ok &&
      !isEqual(parsed.properties, notePropertiesFromPropertyRows(rows)))
  ) {
    return composeNoteContentFromPropertyRows(rows, body)
  }
  const newline = split.prefix.includes("\r\n") ? "\r\n" : "\n"
  const fenceEnd = split.prefix.endsWith("\n") || body === "" ? "" : newline
  const blankLines = split.body.match(/^(?:\r?\n)*/)![0]
  return split.prefix + fenceEnd + blankLines + body
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
    const rowError =
      authoredListPropertyValidationErrorForPropertyRow(row) ??
      authoredNoteLevelValidationErrorForPropertyRow(row)
    if (rowError) {
      return { ok: false, message: rowError }
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

/** Appends an empty-key wiki-link property row; undefined when content cannot be updated. */
export function appendWikiLinkPropertyRow(
  content: string,
  linkText: string
): string | undefined {
  const parsed = parseNoteContentMarkdown(content ?? "")
  if (!parsed.ok) return
  const rows = [
    ...propertyRowsFromNoteProperties(parsed.properties),
    propertyRowWithScalar("", linkText),
  ]
  if (!validatePropertyRowsForRichEdit(rows).ok) return
  return composeNoteContentFromPropertyRows(rows, parsed.body)
}
