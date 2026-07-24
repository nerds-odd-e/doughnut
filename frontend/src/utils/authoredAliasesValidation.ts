import type { PropertyValue } from "@/utils/noteProperties"

export const AUTHORED_ALIASES_MESSAGE =
  "aliases must be a one-level YAML list of nonblank plain alias strings or well-formed wiki-link overlap declarations."

const INVALID_ALIAS_CHARACTERS = /[|#^:]|\\|\/|＼|／|[\r\n]/

/** Whole-item wiki-link token — mirrors WikiLinkMarkdown.INNER_LINK_PATTERN.matches(). */
const WHOLE_WIKI_LINK_ALIAS = /^\[\[([^\]]+)]]$/

export function isAliasesPropertyKey(key: string): boolean {
  return key.trim().toLowerCase() === "aliases"
}

function isWikiLinkAliasItem(trimmed: string): boolean {
  const match = WHOLE_WIKI_LINK_ALIAS.exec(trimmed)
  const captured = match?.[1]
  if (captured === undefined) return false
  const inner = captured.trim()
  if (inner === "") return false
  const pipe = inner.indexOf("|")
  const target = (pipe === -1 ? inner : inner.slice(0, pipe)).trim()
  return target !== ""
}

function isValidPlainAliasText(trimmed: string): boolean {
  if (trimmed.includes("[[") || trimmed.includes("]]")) return false
  return !INVALID_ALIAS_CHARACTERS.test(trimmed)
}

function isAcceptableAuthoredAliasItem(trimmed: string): boolean {
  return isWikiLinkAliasItem(trimmed) || isValidPlainAliasText(trimmed)
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
