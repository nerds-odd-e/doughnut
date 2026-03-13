import { reactive } from "vue"
import type { NoteSearchResult } from "@generated/doughnut-backend-api"

export interface DisplayState {
  showRecentNotes: boolean
  showEmptyState: boolean
  showSearchResults: boolean
  title: string | null
  emptyMessage?: string
  containerClass: string
}

export class SearchResultsModel {
  private state = reactive({
    isSearchInProgress: false,
    cache: {
      global: {} as Record<string, NoteSearchResult[]>,
      local: {} as Record<string, NoteSearchResult[]>,
    },
    recentResult: undefined as NoteSearchResult[] | undefined,
    previousSearchResult: undefined as NoteSearchResult[] | undefined,
    recentNotes: [] as NoteSearchResult[],
  })

  get isSearchInProgress(): boolean {
    return this.state.isSearchInProgress
  }

  get recentNotes(): NoteSearchResult[] {
    return this.state.recentNotes
  }

  set recentNotes(notes: NoteSearchResult[]) {
    this.state.recentNotes = notes
  }

  startSearch(): void {
    this.state.isSearchInProgress = true
  }

  completeSearch(): void {
    this.state.isSearchInProgress = false
  }

  getCachedSearches(isGlobal: boolean): Record<string, NoteSearchResult[]> {
    return isGlobal ? this.state.cache.global : this.state.cache.local
  }

  getCachedResult(
    searchKey: string,
    isGlobal: boolean
  ): NoteSearchResult[] | undefined {
    return this.getCachedSearches(isGlobal)[searchKey]
  }

  setCachedResult(
    searchKey: string,
    isGlobal: boolean,
    results: NoteSearchResult[]
  ): void {
    this.getCachedSearches(isGlobal)[searchKey] = results
    this.state.recentResult = results
  }

  getSearchResult(
    trimmedSearchKey: string,
    isGlobal: boolean
  ): NoteSearchResult[] | undefined {
    const cachedResult = this.getCachedResult(trimmedSearchKey, isGlobal)
    const resultToUse =
      cachedResult ??
      this.state.recentResult ??
      (trimmedSearchKey !== "" ? this.state.previousSearchResult : undefined)

    return resultToUse
  }

  getDistanceById(
    trimmedSearchKey: string,
    isGlobal: boolean
  ): Record<string, number> {
    const map: Record<string, number> = {}
    const cachedResult = this.getCachedResult(trimmedSearchKey, isGlobal)
    const list =
      cachedResult ??
      this.state.recentResult ??
      this.state.previousSearchResult ??
      []
    list.forEach((r) => {
      const id = String(r.noteTopology.id as number)
      const d = r.distance
      if (d != null) map[id] = d
    })
    return map
  }

  prepareForNewSearch(trimmedSearchKey: string, isGlobal: boolean): void {
    const cachedSearches = this.getCachedSearches(isGlobal)
    if (!cachedSearches[trimmedSearchKey]) {
      // Save current result as previous before starting new search
      if (this.state.recentResult !== undefined) {
        this.state.previousSearchResult = this.state.recentResult
      } else {
        const cachedKeys = Object.keys(cachedSearches).sort()
        if (cachedKeys.length > 0) {
          const lastKey = cachedKeys[cachedKeys.length - 1]
          if (lastKey) {
            this.state.previousSearchResult = cachedSearches[lastKey]
          }
        }
      }
      this.state.recentResult = undefined
    }
  }

  clearPreviousResult(): void {
    this.state.previousSearchResult = undefined
  }

  clearRecentResult(): void {
    this.state.recentResult = undefined
  }

  hasPreviousResult(): boolean {
    return this.state.previousSearchResult !== undefined
  }

  getDisplayState(opts: {
    trimmedSearchKey: string
    isGlobal: boolean
    noteId: number | undefined
    isDropdown: boolean
    filteredRecentNotesCount: number
  }): DisplayState {
    const searchResult = this.getSearchResult(
      opts.trimmedSearchKey,
      opts.isGlobal
    )
    const hasSearchKey = opts.trimmedSearchKey !== ""
    const hasSearchResults = searchResult !== undefined
    const hasRecentNotes = opts.filteredRecentNotesCount > 0
    const isWaitingForFirstSearch =
      !hasSearchResults &&
      this.state.previousSearchResult === undefined &&
      hasSearchKey
    const shouldShowRecent =
      (opts.isGlobal || opts.noteId) &&
      (!hasSearchKey || isWaitingForFirstSearch) &&
      !hasSearchResults

    const containerClass = opts.isDropdown ? "dropdown-list" : "result-section"

    if (shouldShowRecent && hasRecentNotes) {
      return {
        showRecentNotes: true,
        showEmptyState: false,
        showSearchResults: false,
        title: "Recently updated notes",
        containerClass,
      }
    }

    if (hasSearchResults && searchResult.length > 0) {
      return {
        showRecentNotes: false,
        showEmptyState: false,
        showSearchResults: true,
        title: "Search result",
        containerClass,
      }
    }

    if (!this.state.isSearchInProgress) {
      if (hasSearchResults && searchResult.length === 0) {
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

  mergeAndCacheResults(opts: {
    trimmedSearchKey: string
    isGlobal: boolean
    literalResults: NoteSearchResult[]
    semanticResults: NoteSearchResult[]
    currentNotebookId?: number
  }): void {
    const combined = [...opts.literalResults, ...opts.semanticResults]
    const existing =
      this.getCachedResult(opts.trimmedSearchKey, opts.isGlobal) ?? []
    const merged = this.mergeUniqueAndSortByDistance(
      existing,
      combined,
      opts.currentNotebookId
    )
    this.setCachedResult(opts.trimmedSearchKey, opts.isGlobal, merged)
    this.clearPreviousResult()
  }

  private mergeUniqueAndSortByDistance(
    existing: NoteSearchResult[],
    incoming: NoteSearchResult[],
    currentNotebookId?: number
  ): NoteSearchResult[] {
    const byId = new Map<number, NoteSearchResult>()
    const getId = (r: NoteSearchResult) => r.noteTopology.id as number

    const chooseBetter = (a: NoteSearchResult, b: NoteSearchResult) => {
      const da = a.distance ?? Infinity
      const db = b.distance ?? Infinity
      return db < da ? b : a
    }

    existing.forEach((r) => byId.set(getId(r), r))
    incoming.forEach((r) => {
      const id = getId(r)
      const prev = byId.get(id)
      byId.set(id, prev ? chooseBetter(prev, r) : r)
    })

    return Array.from(byId.values()).sort((a, b) => {
      const distDiff = (a.distance ?? Infinity) - (b.distance ?? Infinity)
      if (distDiff !== 0) return distDiff
      if (currentNotebookId === undefined) return 0
      const aSame = a.notebookId === currentNotebookId
      const bSame = b.notebookId === currentNotebookId
      return Number(bSame) - Number(aSame)
    })
  }
}
