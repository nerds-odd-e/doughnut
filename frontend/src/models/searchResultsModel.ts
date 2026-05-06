import { reactive } from "vue"
import type {
  NoteSearchResult,
  RelationshipLiteralSearchHit,
} from "@generated/doughnut-backend-api"
import { relationshipLiteralSearchHitKey } from "./relationshipLiteralSearchHitKey"

export interface DisplayState {
  showRecentNotes: boolean
  showEmptyState: boolean
  showSearchResults: boolean
  title: string | null
  emptyMessage?: string
  containerClass: string
}

function noteHitFromSemantic(
  n: NoteSearchResult
): RelationshipLiteralSearchHit {
  return { hitKind: "NOTE", noteSearchResult: n }
}

function hitDistance(h: RelationshipLiteralSearchHit): number {
  if (h.hitKind === "NOTE" && h.noteSearchResult) {
    return h.noteSearchResult.distance ?? Number.POSITIVE_INFINITY
  }
  if (h.hitKind === "FOLDER" || h.hitKind === "NOTEBOOK") {
    return h.distance ?? Number.POSITIVE_INFINITY
  }
  return Number.POSITIVE_INFINITY
}

function titleForSort(h: RelationshipLiteralSearchHit): string {
  if (h.hitKind === "NOTE" && h.noteSearchResult?.noteTopology?.title) {
    return h.noteSearchResult.noteTopology.title.trim().toLowerCase()
  }
  if (h.hitKind === "FOLDER" && h.folderName) {
    return h.folderName.trim().toLowerCase()
  }
  if (h.hitKind === "NOTEBOOK" && h.notebookName) {
    return h.notebookName.trim().toLowerCase()
  }
  return ""
}

function notebookIdOf(h: RelationshipLiteralSearchHit): number | undefined {
  if (h.hitKind === "NOTE" && h.noteSearchResult) {
    return h.noteSearchResult.notebookId
  }
  if (h.hitKind === "FOLDER" || h.hitKind === "NOTEBOOK") {
    return h.notebookId
  }
  return
}

function hitKindRank(kind: RelationshipLiteralSearchHit["hitKind"]): number {
  if (kind === "NOTE") return 0
  if (kind === "NOTEBOOK") return 1
  if (kind === "FOLDER") return 2
  return 3
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

    const containerClass = opts.isDropdown
      ? "dropdown-section"
      : "result-section"

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
    const merged = this.mergeUniqueAndSortByDistance(
      existing,
      incoming,
      opts.currentNotebookId,
      opts.trimmedSearchKey.trim().toLowerCase()
    )
    this.setCachedResult(opts.trimmedSearchKey, opts.isGlobal, merged)
    this.clearPreviousResult()
  }

  private mergeUniqueAndSortByDistance(
    existing: RelationshipLiteralSearchHit[],
    incoming: RelationshipLiteralSearchHit[],
    currentNotebookId?: number,
    searchKeyLower = ""
  ): RelationshipLiteralSearchHit[] {
    const byKey = new Map<string, RelationshipLiteralSearchHit>()

    const isExactLiteralDistance = (d: number) => d === 0

    const chooseBetterNote = (a: NoteSearchResult, b: NoteSearchResult) => {
      const da = a.distance ?? Number.POSITIVE_INFINITY
      const db = b.distance ?? Number.POSITIVE_INFINITY
      if (isExactLiteralDistance(da) && !isExactLiteralDistance(db)) return a
      if (!isExactLiteralDistance(da) && isExactLiteralDistance(db)) return b
      return db < da ? b : a
    }

    const mergeHit = (
      prev: RelationshipLiteralSearchHit | undefined,
      next: RelationshipLiteralSearchHit
    ): RelationshipLiteralSearchHit => {
      if (!prev) return next
      if (
        prev.hitKind === "NOTE" &&
        next.hitKind === "NOTE" &&
        prev.noteSearchResult &&
        next.noteSearchResult
      ) {
        const better = chooseBetterNote(
          prev.noteSearchResult,
          next.noteSearchResult
        )
        return better === prev.noteSearchResult ? prev : next
      }
      if (prev.hitKind === "FOLDER" && next.hitKind === "FOLDER") {
        const da = prev.distance ?? Number.POSITIVE_INFINITY
        const db = next.distance ?? Number.POSITIVE_INFINITY
        if (isExactLiteralDistance(da) && !isExactLiteralDistance(db))
          return prev
        if (!isExactLiteralDistance(da) && isExactLiteralDistance(db))
          return next
        return db < da ? next : prev
      }
      if (prev.hitKind === "NOTEBOOK" && next.hitKind === "NOTEBOOK") {
        const da = prev.distance ?? Number.POSITIVE_INFINITY
        const db = next.distance ?? Number.POSITIVE_INFINITY
        if (isExactLiteralDistance(da) && !isExactLiteralDistance(db))
          return prev
        if (!isExactLiteralDistance(da) && isExactLiteralDistance(db))
          return next
        return db < da ? next : prev
      }
      return next
    }

    existing.forEach((h) => byKey.set(relationshipLiteralSearchHitKey(h), h))
    incoming.forEach((h) => {
      const key = relationshipLiteralSearchHitKey(h)
      const prev = byKey.get(key)
      byKey.set(key, mergeHit(prev, h))
    })

    return Array.from(byKey.values()).sort((a, b) => {
      const ta = titleForSort(a)
      const tb = titleForSort(b)
      const exactA = searchKeyLower !== "" && ta === searchKeyLower
      const exactB = searchKeyLower !== "" && tb === searchKeyLower
      if (exactA !== exactB) return exactA ? -1 : 1

      const da = hitDistance(a)
      const db = hitDistance(b)
      const distDiff = da - db
      if (!Number.isNaN(distDiff) && Math.abs(distDiff) > 1e-6) {
        return distDiff
      }

      if (currentNotebookId !== undefined) {
        const aNb = notebookIdOf(a)
        const bNb = notebookIdOf(b)
        const aSame = aNb === currentNotebookId
        const bSame = bNb === currentNotebookId
        const nb = Number(bSame) - Number(aSame)
        if (nb !== 0) return nb
      }

      const lenDiff = ta.length - tb.length
      if (lenDiff !== 0) return lenDiff
      const cmp = ta.localeCompare(tb, undefined, {
        sensitivity: "base",
      })
      if (cmp !== 0) return cmp

      if (a.hitKind === "NOTE" && b.hitKind === "NOTE") {
        return (
          (a.noteSearchResult!.noteTopology!.id as number) -
          (b.noteSearchResult!.noteTopology!.id as number)
        )
      }
      if (a.hitKind === "FOLDER" && b.hitKind === "FOLDER") {
        return (a.folderId ?? 0) - (b.folderId ?? 0)
      }
      if (a.hitKind === "NOTEBOOK" && b.hitKind === "NOTEBOOK") {
        return (a.notebookId ?? 0) - (b.notebookId ?? 0)
      }
      return hitKindRank(a.hitKind) - hitKindRank(b.hitKind)
    })
  }
}
