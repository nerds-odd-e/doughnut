import type { WikidataSearchEntity } from "@generated/donut-backend-api"

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
