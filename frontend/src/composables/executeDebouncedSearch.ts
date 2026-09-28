import type { SearchTerm } from "@generated/donut-backend-api"
import { SearchController } from "@generated/donut-backend-api/sdk.gen"
import type { SearchResultsModel } from "@/models/searchResultsModel"
import { appendSearchKeyToHistory } from "@/utils/searchKeyHistory"

export async function executeDebouncedSearch(opts: {
  model: SearchResultsModel
  term: SearchTerm
  noteId: number | undefined
  notebookId: number | undefined
  isStillCurrent: () => boolean
}): Promise<void> {
  const snapshotTrimmed = opts.term.searchKey.trim()
  const snapshotGlobal = opts.term.allMyNotebooksAndSubscriptions === true

  if (
    snapshotTrimmed !== "" &&
    opts.model.isImpliedEmptyByShorterPhrase(snapshotTrimmed, snapshotGlobal)
  ) {
    if (!opts.isStillCurrent()) return
    opts.model.mergeAndCacheResults({
      trimmedSearchKey: snapshotTrimmed,
      isGlobal: snapshotGlobal,
      results: [],
      currentNotebookId: opts.notebookId,
    })
    opts.model.completeSearch()
    return
  }

  const { data, error } = opts.noteId
    ? await SearchController.searchForRelationshipTargetWithin({
        path: { note: opts.noteId },
        body: opts.term,
      })
    : await SearchController.searchForRelationshipTarget({ body: opts.term })

  if (!opts.isStillCurrent()) return
  opts.model.mergeAndCacheResults({
    trimmedSearchKey: snapshotTrimmed,
    isGlobal: snapshotGlobal,
    results: error ? [] : data || [],
    currentNotebookId: opts.notebookId,
  })
  opts.model.completeSearch()
  if (snapshotTrimmed !== "") {
    appendSearchKeyToHistory(opts.term.searchKey)
  }
}
