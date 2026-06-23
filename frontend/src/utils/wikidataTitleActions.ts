import type { WikidataSearchEntity } from "@generated/doughnut-backend-api"
import { appendTitleAlias } from "./noteTitleAliasJoiner"

/**
 * Calculates the new title based on the title action (replace or append).
 * This ensures consistent behavior across different components.
 *
 * @param currentTitle - The current title of the note
 * @param entity - The Wikidata entity with the label to use
 * @param titleAction - The action to perform: "replace" or "append"
 * @returns The new title string
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
