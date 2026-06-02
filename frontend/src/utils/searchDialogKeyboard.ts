import { focusTargetWithin } from "@/utils/focusTarget"

export const searchResultListTestId = "search-result-list"
export const searchResultItemTestId = "search-result-item"
export const searchResultListRowSelector = `[data-testid="${searchResultListTestId}"] [data-testid="${searchResultItemTestId}"]`

export const folderSearchResultTestId = "folder-selector-search-result"
export const folderSearchResultRowSelector = `[data-testid="${folderSearchResultTestId}"]`

function isArrowDownToFirstResult(event: KeyboardEvent): boolean {
  return (
    event.key === "ArrowDown" &&
    !event.isComposing &&
    !event.altKey &&
    !event.ctrlKey &&
    !event.metaKey &&
    !event.shiftKey
  )
}

function focusFirstRow(
  container: Element | null,
  rowSelector: string
): boolean {
  if (!container) return false
  const firstRow = container.querySelector(rowSelector)
  return focusTargetWithin(firstRow)
}

export function handleSearchFieldArrowDownToFirstResult(
  event: KeyboardEvent,
  container: Element | null,
  rowSelector: string,
  options?: { when?: boolean }
): boolean {
  if (options?.when === false) return false
  if (!isArrowDownToFirstResult(event)) return false
  if (!focusFirstRow(container, rowSelector)) return false
  event.preventDefault()
  return true
}
