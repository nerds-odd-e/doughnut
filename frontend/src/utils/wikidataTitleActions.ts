import type { WikidataSearchEntity } from "@generated/doughnut-backend-api"
import { mergeAliasIntoList } from "./frontmatterAliases"
import {
  composeNoteContentMarkdown,
  parseNoteContentMarkdown,
} from "./noteContentFrontmatter"
import {
  isListPropertyValue,
  listPropertyValue,
  type NoteProperties,
} from "./noteProperties"

function findAliasesPropertyKey(
  properties: NoteProperties
): string | undefined {
  for (const key of Object.keys(properties)) {
    if (key.trim().toLowerCase() === "aliases") return key
  }
  return
}

/**
 * Returns updated note markdown with `alias` in frontmatter `aliases`, or null when
 * content is unchanged, unparseable, or aliases is present but not a YAML list.
 */
export function appendAliasToNoteContent(
  contentMarkdown: string,
  alias: string
): string | null {
  const trimmedAlias = alias.trim()
  if (!trimmedAlias) return null

  const parsed = parseNoteContentMarkdown(contentMarkdown)
  if (!parsed.ok) return null

  const aliasesKey = findAliasesPropertyKey(parsed.properties)
  if (!aliasesKey) {
    return composeNoteContentMarkdown({
      properties: {
        ...parsed.properties,
        aliases: listPropertyValue([trimmedAlias]),
      },
      body: parsed.body,
    })
  }

  const existingValue = parsed.properties[aliasesKey]
  if (existingValue === undefined || !isListPropertyValue(existingValue)) {
    return null
  }

  const merged = mergeAliasIntoList(existingValue.items, trimmedAlias)
  if (merged === null) return null

  return composeNoteContentMarkdown({
    properties: {
      ...parsed.properties,
      [aliasesKey]: listPropertyValue(merged),
    },
    body: parsed.body,
  })
}

/**
 * Calculates the new title for Wikidata replace; append no longer mutates the title.
 */
export function calculateNewTitle(
  currentTitle: string,
  entity: WikidataSearchEntity,
  titleAction: "replace" | "append"
): string {
  if (titleAction === "append") {
    return currentTitle
  }
  return entity.label
}
