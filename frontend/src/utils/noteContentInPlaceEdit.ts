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
 * splicing only the entries whose property rows changed or were removed.
 */
export function composeNoteContentInPlace(
  authored: string,
  rows: readonly PropertyRow[],
  body: string
): string {
  const split = verbatimFrontmatterPrefixAndBody(authored)
  const parsed = parseNoteContentMarkdown(authored)
  const edited = notePropertiesFromPropertyRows(rows)
  if (
    split === null ||
    (parsed.ok &&
      (rows.length === 0 ||
        // Added and renamed keys still re-dump the whole block for now.
        Object.keys(edited).some((key) => !(key in parsed.properties))))
  ) {
    return composeNoteContentFromPropertyRows(rows, body)
  }
  const prefix = parsed.ok
    ? frontmatterWithEditedEntries(split, parsed.properties, edited)
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
  edited: NoteProperties
): string {
  const { prefix, yamlStart, yamlEnd } = split
  const yamlRaw = prefix.slice(yamlStart, yamlEnd)
  const pairs = YAML.parseDocument(yamlRaw).contents as YAML.YAMLMap<
    YAML.Scalar,
    YAML.Node | null
  > | null
  let result = prefix
  for (const pair of [...(pairs?.items ?? [])].reverse()) {
    const key = String(pair.key.value)
    if (isEqual(authored[key], edited[key])) continue
    const start = yamlStart + pair.key.range![0]
    const end =
      yamlStart +
      yamlRaw.slice(0, (pair.value ?? pair.key).range![1]).trimEnd().length
    if (edited[key] === undefined) {
      const lineStart = result.lastIndexOf("\n", start - 1) + 1
      const lineEnd = result.indexOf("\n", end) + 1
      result = result.slice(0, lineStart) + result.slice(lineEnd)
    } else {
      const entry = YAML.stringify(
        yamlRecordFromNoteProperties({ [key]: edited[key] }),
        { lineWidth: 0 }
      ).trimEnd()
      result = result.slice(0, start) + entry + result.slice(end)
    }
  }
  return result
}
