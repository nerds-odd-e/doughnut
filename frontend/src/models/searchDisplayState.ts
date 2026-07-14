import type { RelationshipLiteralSearchHit } from "@generated/doughnut-backend-api"
import type { SearchListPreference } from "./searchListPreference"

export interface DisplayState {
  showRecentNotes: boolean
  showEmptyState: boolean
  showSearchResults: boolean
  title: string | null
  emptyMessage?: string
  containerClass: string
}

export function computeSearchDisplayState(opts: {
  trimmedSearchKey: string
  isGlobal: boolean
  noteId: number | undefined
  isDropdown: boolean
  filteredRecentNotesCount: number
  listPreference: SearchListPreference
  searchResult: RelationshipLiteralSearchHit[] | undefined
  isSearchInProgress: boolean
  hasPreviousResult: boolean
}): DisplayState {
  const hasSearchKey = opts.trimmedSearchKey !== ""
  const hasSearchResults = opts.searchResult !== undefined
  const hasRecentNotes = opts.filteredRecentNotesCount > 0
  const isWaitingForFirstSearch =
    !hasSearchResults && !opts.hasPreviousResult && hasSearchKey
  const canShowRecentContext = opts.isGlobal || !!opts.noteId
  const shouldShowRecent =
    canShowRecentContext &&
    (opts.listPreference === "recent"
      ? hasRecentNotes
      : opts.listPreference === "matches"
        ? false
        : (!hasSearchKey || isWaitingForFirstSearch) && !hasSearchResults)

  const containerClass = opts.isDropdown ? "dropdown-section" : "result-section"

  if (shouldShowRecent && hasRecentNotes) {
    return {
      showRecentNotes: true,
      showEmptyState: false,
      showSearchResults: false,
      title: "Recently updated notes",
      containerClass,
    }
  }

  if (hasSearchResults && opts.searchResult!.length > 0) {
    return {
      showRecentNotes: false,
      showEmptyState: false,
      showSearchResults: true,
      title: "Search result",
      containerClass,
    }
  }

  if (!opts.isSearchInProgress) {
    if (hasSearchResults && opts.searchResult!.length === 0) {
      let emptyMessage = "No matching notes found."
      if (opts.isDropdown && !hasSearchKey) {
        emptyMessage = opts.noteId
          ? "No recent notes found."
          : "Similar notes within the same notebook"
      }
      return {
        showRecentNotes: false,
        showEmptyState: true,
        showSearchResults: false,
        title: "Search result",
        emptyMessage,
        containerClass,
      }
    }

    if (!hasSearchKey && opts.noteId && opts.isDropdown && !hasRecentNotes) {
      return {
        showRecentNotes: false,
        showEmptyState: true,
        showSearchResults: false,
        title: "Recently updated notes",
        emptyMessage: "No recent notes found.",
        containerClass,
      }
    }
  }

  return {
    showRecentNotes: false,
    showEmptyState: false,
    showSearchResults: false,
    title: null,
    containerClass,
  }
}
