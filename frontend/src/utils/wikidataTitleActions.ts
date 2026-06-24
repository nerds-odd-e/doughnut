import type { WikidataSearchEntity } from "@generated/doughnut-backend-api"
import {
  composeNoteContentMarkdown,
  parseNoteContentMarkdown,
} from "./noteContentFrontmatter"
import { appendTitleAlias } from "./noteTitleAliasJoiner"
import { listPropertyValue, type NoteProperties } from "./noteProperties"

function noteHasAliasesProperty(properties: NoteProperties): boolean {
  for (const key of Object.keys(properties)) {
    if (key.trim().toLowerCase() === "aliases") return true
  }
  return false
}

/**
 * When the note has no existing `aliases` frontmatter property, returns markdown
 * with a new YAML `aliases` list containing `alias`. Otherwise returns null.
 */
export function appendAliasToNoteContentWhenAbsent(
  contentMarkdown: string,
  alias: string
): string | null {
  const trimmedAlias = alias.trim()
  if (!trimmedAlias) return null

  const parsed = parseNoteContentMarkdown(contentMarkdown)
  if (!parsed.ok || noteHasAliasesProperty(parsed.properties)) return null

  return composeNoteContentMarkdown({
    properties: {
      ...parsed.properties,
      aliases: listPropertyValue([trimmedAlias]),
    },
    body: parsed.body,
  })
}

/**
 * Calculates the new title for replace, or legacy title-alias append when
 * frontmatter `aliases` is not used (e.g. new-note form).
 */
export function calculateNewTitle(
  currentTitle: string,
  entity: WikidataSearchEntity,
  titleAction: "replace" | "append"
): string {
  if (titleAction === "replace") {
    return entity.label
  }
  return appendTitleAlias(currentTitle, entity.label)
}
