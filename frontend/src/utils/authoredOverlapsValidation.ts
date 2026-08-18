import type { PropertyValue } from "@/utils/noteProperties"
import { isWellFormedWholeWikiLinkItem } from "@/utils/authoredLinkMarkup"

export const AUTHORED_OVERLAPS_MESSAGE =
  "overlaps must be a one-level YAML list of well-formed wiki-link items."

export function isOverlapsPropertyKey(key: string): boolean {
  return key.trim().toLowerCase() === "overlaps"
}

export function authoredOverlapsValidationErrorForPropertyValue(
  value: PropertyValue
): string | undefined {
  if (value.kind === "scalar") {
    return AUTHORED_OVERLAPS_MESSAGE
  }
  for (const item of value.items) {
    const trimmed = item.trim()
    if (trimmed === "" || !isWellFormedWholeWikiLinkItem(trimmed)) {
      return AUTHORED_OVERLAPS_MESSAGE
    }
  }
  return
}

export function authoredOverlapsValidationErrorForPropertyRow(row: {
  key: string
  value: PropertyValue
}): string | undefined {
  if (!isOverlapsPropertyKey(row.key)) return
  return authoredOverlapsValidationErrorForPropertyValue(row.value)
}
