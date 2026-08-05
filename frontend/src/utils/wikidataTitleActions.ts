import type { WikidataSearchEntity } from "@generated/doughnut-backend-api"
import { appendItemToFrontmatterStringList } from "./frontmatterStringList"

/**
 * Returns updated note markdown with `alias` in frontmatter `aliases`, or null when
 * content is unchanged, unparseable, or aliases is present but not a YAML list.
 */
export function appendAliasToNoteContent(
  contentMarkdown: string,
  alias: string
): string | null {
  return appendItemToFrontmatterStringList(contentMarkdown, "aliases", alias)
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
