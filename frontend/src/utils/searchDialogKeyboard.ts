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

interface InputListKeydownOptions {
  input: HTMLElement | null
  targets: HTMLElement[]
  focusWithinTarget?: boolean
}

export function handleInputListKeydown(
  event: KeyboardEvent,
  { input, targets, focusWithinTarget = false }: InputListKeydownOptions
): boolean {
  if (!isVerticalListNavigationKey(event) || targets.length === 0) return false

  const active = document.activeElement
  const inInput = input != null && active === input
  const currentIndex = targets.findIndex((target) =>
    focusWithinTarget ? target.contains(active) : target === active
  )
  if (!inInput && currentIndex === -1) return false

  const nextIndex =
    event.key === "ArrowDown" ? currentIndex + 1 : currentIndex - 1
  if (event.key === "ArrowUp" && currentIndex === 0) {
    input?.focus()
  } else {
    const target = targets[nextIndex]
    if (!target || (inInput && event.key === "ArrowUp")) return false
    if (focusWithinTarget) {
      if (!focusTargetWithin(target)) return false
    } else {
      target.focus()
    }
  }
  event.preventDefault()
  return true
}

export function handleSearchDialogListKeydown(
  event: KeyboardEvent,
  options: SearchDialogListKeydownOptions
): boolean {
  if (options.when === false) return false
  return handleInputListKeydown(event, {
    input: options.searchInput,
    targets: Array.from(
      options.container?.querySelectorAll<HTMLElement>(options.rowSelector) ??
        []
    ),
    focusWithinTarget: true,
  })
}

export function bindSearchDialogListKeydown(
  getOptions: () => SearchDialogListKeydownOptions
): (event: KeyboardEvent) => void {
  return (event) => handleSearchDialogListKeydown(event, getOptions())
}
