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

/** First `key: value` scalar in a YAML block (line-based; case-insensitive key; trim; strip matching outer quotes). */
export function firstScalarValueFromYamlBlock(
  yamlRaw: string,
  fieldKey: string
): string | undefined {
  if (!yamlRaw) return
  const normalized = yamlRaw.replace(/\r\n/g, "\n").replace(/\r/g, "\n")
  const needle = fieldKey.toLowerCase()
  for (const line of normalized.split("\n")) {
    const t = line.trim()
    if (t === "" || t.startsWith("#")) continue
    const colon = t.indexOf(":")
    if (colon < 0) continue
    const key = t.slice(0, colon).trim()
    if (key.toLowerCase() !== needle) continue
    let value = t.slice(colon + 1).trim()
    if (value === "") return
    if (value.length >= 2) {
      const first = value[0]!
      const last = value[value.length - 1]!
      if ((first === '"' && last === '"') || (first === "'" && last === "'")) {
        value = value.slice(1, -1).trim()
      }
    }
    return value
  }
  return
}

/** Resolved header image URL and optional mask from note Markdown (leading frontmatter only). */
export function noteImageScalarsFromMarkdown(markdown: string): {
  noteImage?: string
  imageMask?: string
} {
  const split = splitLeadingFrontmatter(markdown)
  if (split.kind !== "parsed") return {}
  const noteImage = firstScalarValueFromYamlBlock(split.yamlRaw, "image")
  const imageMask = firstScalarValueFromYamlBlock(split.yamlRaw, "image_mask")
  const out: { noteImage?: string; imageMask?: string } = {}
  if (noteImage !== undefined && noteImage !== "") out.noteImage = noteImage
  if (imageMask !== undefined && imageMask !== "") out.imageMask = imageMask
  return out
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
