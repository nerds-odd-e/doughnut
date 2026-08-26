import type { SearchTerm } from "@generated/donut-backend-api"
import { SearchController } from "@generated/donut-backend-api/sdk.gen"
import type { SearchResultsModel } from "@/models/searchResultsModel"
import { appendSearchKeyToHistory } from "@/utils/searchKeyHistoryCookie"

export async function executeDebouncedSearch(opts: {
  model: SearchResultsModel
  term: SearchTerm
  noteId: number | undefined
  notebookId: number | undefined
  semanticEnabled: boolean
  isStillCurrent: () => boolean
}): Promise<void> {
  const snapshotTrimmed = opts.term.searchKey.trim()
  const snapshotGlobal = opts.term.allMyNotebooksAndSubscriptions === true

  if (
    !opts.semanticEnabled &&
    snapshotTrimmed !== "" &&
    opts.model.isImpliedEmptyByShorterPhrase(snapshotTrimmed, snapshotGlobal)
  ) {
    if (!opts.isStillCurrent()) return
    opts.model.mergeAndCacheResults({
      trimmedSearchKey: snapshotTrimmed,
      isGlobal: snapshotGlobal,
      literalResults: [],
      currentNotebookId: opts.notebookId,
    })
    opts.model.completeSearch()
    return
  }

  const literalPromise = opts.noteId
    ? SearchController.searchForRelationshipTargetWithin({
        path: { note: opts.noteId },
        body: opts.term,
      })
    : SearchController.searchForRelationshipTarget({ body: opts.term })

  const semanticPromise = opts.semanticEnabled
    ? opts.noteId
      ? SearchController.semanticSearchWithin({
          path: { note: opts.noteId },
          body: opts.term,
        })
      : SearchController.semanticSearch({ body: opts.term })
    : null

  literalPromise.then((literalRes) => {
    if (!opts.isStillCurrent()) return
    const literal = literalRes.error ? [] : literalRes.data || []
    opts.model.mergeAndCacheResults({
      trimmedSearchKey: snapshotTrimmed,
      isGlobal: snapshotGlobal,
      literalResults: literal,
      currentNotebookId: opts.notebookId,
    })
  })

  if (semanticPromise) {
    semanticPromise.then((semanticRes) => {
      if (!opts.isStillCurrent()) return
      const semantic = semanticRes.error ? [] : semanticRes.data || []
      opts.model.mergeAndCacheResults({
        trimmedSearchKey: snapshotTrimmed,
        isGlobal: snapshotGlobal,
        semanticResults: semantic,
        currentNotebookId: opts.notebookId,
      })
    })
  }

  if (semanticPromise) {
    await Promise.all([literalPromise, semanticPromise])
  } else {
    await literalPromise
  }
  if (!opts.isStillCurrent()) return
  opts.model.completeSearch()
  if (snapshotTrimmed !== "") {
    appendSearchKeyToHistory(opts.term.searchKey)
  }
}
