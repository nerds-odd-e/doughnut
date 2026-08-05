import type { PropertyValue } from "@/utils/noteProperties"
import { isWellFormedWholeWikiLinkItem } from "@/utils/wholeWikiLinkItem"

export const AUTHORED_ALIASES_MESSAGE =
  "aliases must be a one-level YAML list of nonblank plain alias strings or well-formed wiki-link overlap declarations."

const INVALID_ALIAS_CHARACTERS = /[|#^:]|\\|\/|＼|／|[\r\n]/

export function isAliasesPropertyKey(key: string): boolean {
  return key.trim().toLowerCase() === "aliases"
}

function isValidPlainAliasText(trimmed: string): boolean {
  if (trimmed.includes("[[") || trimmed.includes("]]")) return false
  return !INVALID_ALIAS_CHARACTERS.test(trimmed)
}

function isAcceptableAuthoredAliasItem(trimmed: string): boolean {
  return (
    isWellFormedWholeWikiLinkItem(trimmed) || isValidPlainAliasText(trimmed)
  )
}

export function authoredAliasesValidationErrorForPropertyValue(
  value: PropertyValue
): string | undefined {
  if (value.kind === "scalar") {
    return AUTHORED_ALIASES_MESSAGE
  }
  for (const item of value.items) {
    const trimmed = item.trim()
    if (trimmed === "" || !isAcceptableAuthoredAliasItem(trimmed)) {
      return AUTHORED_ALIASES_MESSAGE
    }
  }
  return
}

export function authoredAliasesValidationErrorForPropertyRow(row: {
  key: string
  value: PropertyValue
}): string | undefined {
  if (!isAliasesPropertyKey(row.key)) return
  return authoredAliasesValidationErrorForPropertyValue(row.value)
}
