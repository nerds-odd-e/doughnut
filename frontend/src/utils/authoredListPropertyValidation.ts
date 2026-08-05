import {
  authoredAliasesValidationErrorForPropertyValue,
  isAliasesPropertyKey,
} from "@/utils/authoredAliasesValidation"
import type { PropertyValue } from "@/utils/noteProperties"

/**
 * Keys that use the shared authored string-list property UX (insert-as-list +
 * list validation). Add further keys here without forking rich-list insert/popup paths.
 */
export function isAuthoredListPropertyKey(key: string): boolean {
  return isAliasesPropertyKey(key)
}

export function authoredListPropertyValidationErrorForPropertyValue(
  key: string,
  value: PropertyValue
): string | undefined {
  if (isAliasesPropertyKey(key)) {
    return authoredAliasesValidationErrorForPropertyValue(value)
  }
  return
}

export function authoredListPropertyValidationErrorForPropertyRow(row: {
  key: string
  value: PropertyValue
}): string | undefined {
  if (!isAuthoredListPropertyKey(row.key)) return
  return authoredListPropertyValidationErrorForPropertyValue(row.key, row.value)
}
