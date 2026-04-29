import YAML from "yaml"

export type ParseNoteDetailsFailureReason =
  | "malformed_frontmatter_fence"
  | "malformed_yaml"
  | "duplicate_keys"
  | "unsupported_value"

export type ParseNoteDetailsMarkdownResult =
  | { ok: true; properties: Record<string, string>; body: string }
  | {
      ok: false
      reason: ParseNoteDetailsFailureReason
      message: string
    }

function stripBom(s: string): string {
  return s.charCodeAt(0) === 0xfeff ? s.slice(1) : s
}

function splitLeadingFrontmatter(
  details: string
):
  | { kind: "none" }
  | { kind: "parsed"; yamlRaw: string; body: string }
  | { kind: "invalid"; message: string } {
  const text = stripBom(details)
  const lines = text.split(/\r?\n/)

  if (lines[0] !== "---") {
    return { kind: "none" }
  }

  for (let i = 1; i < lines.length; i++) {
    if (lines[i] === "---") {
      const yamlRaw = lines.slice(1, i).join("\n")
      const body = lines.slice(i + 1).join("\n")
      return { kind: "parsed", yamlRaw, body }
    }
  }

  return {
    kind: "invalid",
    message:
      "Opening --- was found without a closing --- line for YAML frontmatter.",
  }
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
  | Extract<ParseNoteDetailsMarkdownResult, { ok: false }> {
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

/** Parses leading YAML frontmatter from persisted note Markdown details. */
export function parseNoteDetailsMarkdown(
  details: string
): ParseNoteDetailsMarkdownResult {
  const split = splitLeadingFrontmatter(details)
  if (split.kind === "none") {
    return { ok: true, properties: {}, body: details }
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

/** Composes note Markdown details with optional leading YAML frontmatter. */
export function composeNoteDetailsMarkdown(input: {
  properties: Record<string, string>
  body: string
}): string {
  const keys = Object.keys(input.properties)
  if (keys.length === 0) {
    return input.body
  }

  const yamlBlock = YAML.stringify(input.properties, {
    lineWidth: 0,
  }).trimEnd()

  return `---\n${yamlBlock}\n---\n${input.body}`
}
