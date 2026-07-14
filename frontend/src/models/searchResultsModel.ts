import { reactive } from "vue"
import type {
  NoteSearchResult,
  RelationshipLiteralSearchHit,
} from "@generated/doughnut-backend-api"
import {
  computeSearchDisplayState,
  type DisplayState,
} from "./searchDisplayState"
import { mergeRelationshipLiteralSearchHits } from "./mergeRelationshipLiteralSearchHits"
import type { SearchListPreference } from "./searchListPreference"

export type { DisplayState }

function noteHitFromSemantic(
  n: NoteSearchResult
): RelationshipLiteralSearchHit {
  return { hitKind: "NOTE", noteSearchResult: n }
}

export class SearchResultsModel {
  private state = reactive({
    isSearchInProgress: false,
    cache: {
      global: {} as Record<string, RelationshipLiteralSearchHit[]>,
      local: {} as Record<string, RelationshipLiteralSearchHit[]>,
    },
    recentResult: undefined as RelationshipLiteralSearchHit[] | undefined,
    previousSearchResult: undefined as
      | RelationshipLiteralSearchHit[]
      | undefined,
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

  clearSearchCaches(): void {
    this.state.cache.global = {}
    this.state.cache.local = {}
    this.state.recentResult = undefined
    this.state.previousSearchResult = undefined
  }

  getCachedSearches(
    isGlobal: boolean
  ): Record<string, RelationshipLiteralSearchHit[]> {
    return isGlobal ? this.state.cache.global : this.state.cache.local
  }

  getCachedResult(
    searchKey: string,
    isGlobal: boolean
  ): RelationshipLiteralSearchHit[] | undefined {
    return this.getCachedSearches(isGlobal)[searchKey]
  }

  setCachedResult(
    searchKey: string,
    isGlobal: boolean,
    results: RelationshipLiteralSearchHit[]
  ) {
    this.getCachedSearches(isGlobal)[searchKey] = results
    this.state.recentResult = results
  }

  getSearchResult(
    trimmedSearchKey: string,
    isGlobal: boolean
  ): RelationshipLiteralSearchHit[] | undefined {
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
    list.forEach((h) => {
      if (h.hitKind !== "NOTE" || !h.noteSearchResult?.noteTopology?.id) {
        return
      }
      const id = String(h.noteSearchResult.noteTopology.id as number)
      const d = h.noteSearchResult.distance
      if (d != null) map[id] = d
    })
    return map
  }

  prepareForNewSearch(trimmedSearchKey: string, isGlobal: boolean): void {
    const cachedSearches = this.getCachedSearches(isGlobal)
    if (!cachedSearches[trimmedSearchKey]) {
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
    listPreference?: SearchListPreference
  }): DisplayState {
    return computeSearchDisplayState({
      ...opts,
      listPreference: opts.listPreference ?? "auto",
      searchResult: this.getSearchResult(opts.trimmedSearchKey, opts.isGlobal),
      isSearchInProgress: this.state.isSearchInProgress,
      hasPreviousResult: this.state.previousSearchResult !== undefined,
    })
  }

  /**
   * Merges new result batches into the cache for this search key.
   * Omit `literalResults` or `semanticResults` (leave undefined) when that
   * request has not completed yet; pass an array (possibly empty) when it has.
   */
  mergeAndCacheResults(opts: {
    trimmedSearchKey: string
    isGlobal: boolean
    literalResults?: RelationshipLiteralSearchHit[]
    semanticResults?: NoteSearchResult[]
    currentNotebookId?: number
  }): void {
    const existing =
      this.getCachedResult(opts.trimmedSearchKey, opts.isGlobal) ?? []
    const incoming: RelationshipLiteralSearchHit[] = []
    if (opts.literalResults !== undefined) {
      incoming.push(...opts.literalResults)
    }
    if (opts.semanticResults !== undefined) {
      incoming.push(...opts.semanticResults.map((n) => noteHitFromSemantic(n)))
    }
    const merged = mergeRelationshipLiteralSearchHits(
      existing,
      incoming,
      opts.currentNotebookId,
      opts.trimmedSearchKey.trim().toLowerCase()
    )
    this.setCachedResult(opts.trimmedSearchKey, opts.isGlobal, merged)
    this.clearPreviousResult()
  }
}
