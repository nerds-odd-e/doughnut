/** One frontmatter property value: scalar today; list items arrive in a later phase. */
export type PropertyValue =
  | { kind: "scalar"; value: string }
  | { kind: "list"; items: readonly string[] }

export type NoteProperties = Record<string, PropertyValue>

export function scalarPropertyValue(value: string): PropertyValue {
  return { kind: "scalar", value }
}

export function listPropertyValue(items: readonly string[]): PropertyValue {
  return { kind: "list", items }
}

export function isScalarPropertyValue(
  value: PropertyValue
): value is { kind: "scalar"; value: string } {
  return value.kind === "scalar"
}

export function isListPropertyValue(
  value: PropertyValue
): value is { kind: "list"; items: readonly string[] } {
  return value.kind === "list"
}

/** Scalar string when the value is scalar; undefined for lists and future unsupported kinds. */
export function scalarStringFromPropertyValue(
  value: PropertyValue
): string | undefined {
  return value.kind === "scalar" ? value.value : undefined
}

/** Builds note properties from legacy scalar-only records. */
export function notePropertiesFromScalarRecord(
  record: Record<string, string>
): NoteProperties {
  const properties: NoteProperties = {}
  for (const key of Object.keys(record)) {
    properties[key] = scalarPropertyValue(record[key]!)
  }
  return properties
}

/** Flattens scalar properties to a string record; omits non-scalar values. */
export function scalarRecordFromNoteProperties(
  properties: NoteProperties
): Record<string, string> {
  const record: Record<string, string> = {}
  for (const key of Object.keys(properties)) {
    const scalar = scalarStringFromPropertyValue(properties[key]!)
    if (scalar !== undefined) record[key] = scalar
  }
  return record
}

/** Case-insensitive trimmed key lookup for a scalar frontmatter field. */
export function frontmatterScalar(
  properties: NoteProperties,
  fieldKey: string
): string | undefined {
  const needle = fieldKey.toLowerCase()
  for (const key of Object.keys(properties)) {
    if (key.trim().toLowerCase() !== needle) continue
    const scalar = scalarStringFromPropertyValue(properties[key]!)
    const trimmed = scalar?.trim()
    if (trimmed) return trimmed
  }
  return
}

export function yamlScalarToPropertyValue(
  value: unknown
): PropertyValue | null {
  if (value === null || value === undefined) return null
  if (typeof value === "string") return scalarPropertyValue(value)
  if (typeof value === "number" || typeof value === "boolean") {
    return scalarPropertyValue(String(value))
  }
  if (typeof value === "bigint") return scalarPropertyValue(String(value))
  return null
}
