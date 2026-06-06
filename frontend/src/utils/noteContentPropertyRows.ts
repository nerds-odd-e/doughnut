import { composeNoteContentMarkdown } from "@/utils/noteContentFrontmatter"

/** One key/value row for rich property editing (order matches sorted keys). */
export type PropertyRow = { key: string; value: string }

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
