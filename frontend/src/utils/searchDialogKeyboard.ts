import { focusTargetWithin } from "@/utils/focusTarget"

export const searchResultListTestId = "search-result-list"
export const searchResultItemTestId = "search-result-item"
export const searchResultListRowSelector = `[data-testid="${searchResultListTestId}"] [data-testid="${searchResultItemTestId}"]`

export const folderSearchResultTestId = "folder-selector-search-result"
export const folderSearchResultRowSelector = `[data-testid="${folderSearchResultTestId}"]`

export interface SearchDialogListKeydownOptions {
  container: Element | null
  rowSelector: string
  searchInput: HTMLElement | null
  when?: boolean
}

function isVerticalListNavigationKey(event: KeyboardEvent): boolean {
  return (
    (event.key === "ArrowDown" || event.key === "ArrowUp") &&
    !event.isComposing &&
    !event.altKey &&
    !event.ctrlKey &&
    !event.metaKey &&
    !event.shiftKey
  )
}

function listRows(container: Element | null, rowSelector: string): Element[] {
  if (!container) return []
  return Array.from(container.querySelectorAll(rowSelector))
}

function isFocusInSearchField(searchInput: HTMLElement | null): boolean {
  return searchInput != null && document.activeElement === searchInput
}

function isFocusInRows(rows: Element[]): boolean {
  const active = document.activeElement
  if (!active) return false
  return rows.some((row) => row.contains(active))
}

function focusedRowIndex(rows: Element[]): number {
  const active = document.activeElement
  if (!active) return -1
  return rows.findIndex((row) => row.contains(active))
}

function focusRow(row: Element): boolean {
  return focusTargetWithin(row)
}

export function handleSearchDialogListKeydown(
  event: KeyboardEvent,
  options: SearchDialogListKeydownOptions
): boolean {
  if (options.when === false) return false
  if (!isVerticalListNavigationKey(event)) return false

  const { container, rowSelector, searchInput } = options
  const rows = listRows(container, rowSelector)
  if (rows.length === 0) return false

  const inSearch = isFocusInSearchField(searchInput)
  const inList = isFocusInRows(rows)
  if (!inSearch && !inList) return false

  const currentIndex = focusedRowIndex(rows)

  if (event.key === "ArrowDown") {
    if (inSearch) {
      const firstRow = rows[0]
      if (!firstRow || !focusRow(firstRow)) return false
      event.preventDefault()
      return true
    }
    if (inList && currentIndex >= 0 && currentIndex < rows.length - 1) {
      const nextRow = rows[currentIndex + 1]
      if (!nextRow || !focusRow(nextRow)) return false
      event.preventDefault()
      return true
    }
    return false
  }

  if (event.key === "ArrowUp") {
    if (!inList || currentIndex === -1) return false
    if (currentIndex === 0) {
      searchInput?.focus()
      event.preventDefault()
      return true
    }
    const previousRow = rows[currentIndex - 1]
    if (!previousRow || !focusRow(previousRow)) return false
    event.preventDefault()
    return true
  }

  return false
}

export function bindSearchDialogListKeydown(
  getOptions: () => SearchDialogListKeydownOptions
): (event: KeyboardEvent) => void {
  return (event) => handleSearchDialogListKeydown(event, getOptions())
}
