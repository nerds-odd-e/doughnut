import { isEqual } from "es-toolkit"
import YAML from "yaml"
import {
  parseNoteContentMarkdown,
  verbatimFrontmatterPrefixAndBody,
} from "@/utils/noteContentFrontmatterParse"
import {
  type PropertyRow,
  composeNoteContentFromPropertyRows,
  notePropertiesFromPropertyRows,
} from "@/utils/noteContentPropertyRows"
import {
  type NoteProperties,
  yamlRecordFromNoteProperties,
} from "@/utils/noteProperties"

/**
 * Composes `body` after the authored frontmatter text and separator of `authored`,
 * splicing only the entries whose property rows were added, renamed, changed or removed.
 */
export function composeNoteContentInPlace(
  authored: string,
  rows: readonly PropertyRow[],
  body: string
): string {
  const split = verbatimFrontmatterPrefixAndBody(authored)
  const parsed = parseNoteContentMarkdown(authored)
  if (split === null || (parsed.ok && rows.length === 0)) {
    return composeNoteContentFromPropertyRows(rows, body)
  }
  const prefix = parsed.ok
    ? frontmatterWithEditedEntries(split, parsed.properties, rows)
    : split.prefix
  const newline = prefix.includes("\r\n") ? "\r\n" : "\n"
  const fenceEnd = prefix.endsWith("\n") || body === "" ? "" : newline
  const leadingBlankLines = /^(?:\r?\n)*/
  const blankLines = split.body.match(leadingBlankLines)![0]
  return prefix + fenceEnd + blankLines + body.replace(leadingBlankLines, "")
}

function frontmatterWithEditedEntries(
  split: { prefix: string; yamlStart: number; yamlEnd: number },
  authored: NoteProperties,
  rows: readonly PropertyRow[]
): string {
  const { prefix, yamlStart, yamlEnd } = split
  const edited = notePropertiesFromPropertyRows(rows)
  const yamlRaw = prefix.slice(yamlStart, yamlEnd)
  const pairs =
    (
      YAML.parseDocument(yamlRaw).contents as YAML.YAMLMap<
        YAML.Scalar,
        YAML.Node | null
      > | null
    )?.items ?? []
  const keys = pairs.map((pair) => String(pair.key.value))
  // Rows keep the authored order, so an entry whose key left the rows was
  // renamed when the row at its position carries a key not authored before.
  const newKeys = keys.map((key, i) => {
    const row = rows[i]
    return key in edited || !row || row.key in authored ? key : row.key
  })
  const added = Object.entries(edited)
    .filter(([key]) => !newKeys.includes(key))
    .map(([key, value]) => `${entryText(key, value)}\n`)
    .join("")
  let result = prefix.slice(0, yamlEnd) + added + prefix.slice(yamlEnd)
  for (let i = pairs.length - 1; i >= 0; i--) {
    const pair = pairs[i]!
    const key = keys[i]!
    const newKey = newKeys[i]!
    if (newKey === key && isEqual(authored[key], edited[key])) continue
    const start = yamlStart + pair.key.range![0]
    const end =
      yamlStart +
      yamlRaw.slice(0, (pair.value ?? pair.key).range![1]).trimEnd().length
    if (edited[newKey] === undefined) {
      const lineStart = result.lastIndexOf("\n", start - 1) + 1
      const lineEnd = result.indexOf("\n", end) + 1
      result = result.slice(0, lineStart) + result.slice(lineEnd)
    } else if (isEqual(authored[key], edited[newKey])) {
      const keyEnd = yamlStart + pair.key.range![1]
      result =
        result.slice(0, start) +
        YAML.stringify(newKey).trimEnd() +
        result.slice(keyEnd)
    } else {
      result =
        result.slice(0, start) +
        entryText(newKey, edited[newKey]) +
        result.slice(end)
    }
  }
  return result
}

function entryText(key: string, value: NoteProperties[string]) {
  return YAML.stringify(yamlRecordFromNoteProperties({ [key]: value }), {
    lineWidth: 0,
  }).trimEnd()
}
