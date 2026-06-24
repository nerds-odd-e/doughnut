import type { PropertyValue } from "@/utils/noteProperties"

export const AUTHORED_ALIASES_MESSAGE =
  "aliases must be a one-level YAML list of nonblank strings that can safely be used as wiki-link text."

const INVALID_ALIAS_CHARACTERS = /[|#^:]|\\|\/|＼|／|[\r\n]/

export function isAliasesPropertyKey(key: string): boolean {
  return key.trim().toLowerCase() === "aliases"
}

function isValidAliasText(trimmed: string): boolean {
  if (trimmed.includes("[[") || trimmed.includes("]]")) return false
  return !INVALID_ALIAS_CHARACTERS.test(trimmed)
}

export function authoredAliasesValidationErrorForPropertyValue(
  value: PropertyValue
): string | undefined {
  if (value.kind === "scalar") {
    return AUTHORED_ALIASES_MESSAGE
  }
  for (const item of value.items) {
    const trimmed = item.trim()
    if (trimmed === "" || !isValidAliasText(trimmed)) {
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
