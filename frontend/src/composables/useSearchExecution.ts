import type {
  RelationshipLiteralSearchHit,
  SearchTerm,
} from "@generated/doughnut-backend-api"
import {
  NoteController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { debounce } from "mini-debounce"
import {
  computed,
  onBeforeUnmount,
  ref,
  shallowRef,
  watch,
  type Ref,
} from "vue"
import { SearchResultsModel } from "@/models/searchResultsModel"
import { appendSearchKeyToHistory } from "@/utils/searchKeyHistoryCookie"

const SEARCH_DEBOUNCE_MS = 1000

export function useSearchExecution(opts: {
  inputSearchKey: Ref<string>
  noteId: Ref<number | undefined>
  notebookId: Ref<number | undefined>
  semanticSearchEnabled: Ref<boolean>
  allMyNotebooksAndSubscriptions: Ref<boolean>
  allMyCircles: Ref<boolean>
}) {
  const model = new SearchResultsModel()
  const searchGeneration = shallowRef(0)
  const timeoutId = ref<ReturnType<typeof setTimeout>>()
  const oldSearchTerm = ref<SearchTerm>({
    searchKey: "",
    allMyNotebooksAndSubscriptions: true,
    allMyCircles: false,
  })

  const trimmedSearchKey = computed(() => opts.inputSearchKey.value.trim())
  const isGlobalSearch = computed(
    () => opts.allMyNotebooksAndSubscriptions.value === true
  )

  const searchResult = computed(() =>
    model.getSearchResult(trimmedSearchKey.value, isGlobalSearch.value)
  )

  const filteredRecentNotes = computed(() =>
    opts.noteId.value
      ? model.recentNotes.filter(
          (note) => note.noteTopology.id !== opts.noteId.value
        )
      : model.recentNotes
  )

  const recentNotesAsHits = computed((): RelationshipLiteralSearchHit[] =>
    filteredRecentNotes.value.map((r) => ({
      hitKind: "NOTE" as const,
      noteSearchResult: r,
    }))
  )

  const isSearchInProgress = computed(() => model.isSearchInProgress)

  const debounced = debounce((callback) => callback(), SEARCH_DEBOUNCE_MS)

  const fetchRecentNotes = async () => {
    if (
      (isGlobalSearch.value || opts.noteId.value) &&
      model.recentNotes.length === 0
    ) {
      const { data: notes, error } = await NoteController.getRecentNotes({})
      model.recentNotes = error ? [] : notes || []
    }
  }

  const search = () => {
    const originalTrimmedKey = trimmedSearchKey.value
    model.prepareForNewSearch(originalTrimmedKey, isGlobalSearch.value)

    if (
      !model.hasPreviousResult() &&
      (isGlobalSearch.value || opts.noteId.value) &&
      model.recentNotes.length === 0
    ) {
      fetchRecentNotes()
    }

    if (originalTrimmedKey !== "") {
      model.startSearch()
    }

    timeoutId.value = debounced(async () => {
      const gen = ++searchGeneration.value
      const term: SearchTerm = {
        searchKey: opts.inputSearchKey.value,
        allMyNotebooksAndSubscriptions:
          opts.allMyNotebooksAndSubscriptions.value,
        allMyCircles: opts.allMyCircles.value,
      }
      const snapshotTrimmed = term.searchKey.trim()
      const snapshotGlobal = term.allMyNotebooksAndSubscriptions === true
      const snapshotNotebookId = opts.notebookId.value
      const snapshotSemantic = opts.semanticSearchEnabled.value

      const literalPromise = opts.noteId.value
        ? SearchController.searchForRelationshipTargetWithin({
            path: { note: opts.noteId.value },
            body: term,
          })
        : SearchController.searchForRelationshipTarget({ body: term })

      const semanticPromise = snapshotSemantic
        ? opts.noteId.value
          ? SearchController.semanticSearchWithin({
              path: { note: opts.noteId.value },
              body: term,
            })
          : SearchController.semanticSearch({ body: term })
        : null

      const applyIfCurrent = () =>
        gen === searchGeneration.value &&
        snapshotTrimmed === trimmedSearchKey.value &&
        snapshotGlobal === isGlobalSearch.value &&
        snapshotSemantic === opts.semanticSearchEnabled.value

      literalPromise.then((literalRes) => {
        if (!applyIfCurrent()) return
        const literal = literalRes.error ? [] : literalRes.data || []
        model.mergeAndCacheResults({
          trimmedSearchKey: snapshotTrimmed,
          isGlobal: snapshotGlobal,
          literalResults: literal,
          currentNotebookId: snapshotNotebookId,
        })
      })

      if (semanticPromise) {
        semanticPromise.then((semanticRes) => {
          if (!applyIfCurrent()) return
          const semantic = semanticRes.error ? [] : semanticRes.data || []
          model.mergeAndCacheResults({
            trimmedSearchKey: snapshotTrimmed,
            isGlobal: snapshotGlobal,
            semanticResults: semantic,
            currentNotebookId: snapshotNotebookId,
          })
        })
      }

      if (semanticPromise) {
        await Promise.all([literalPromise, semanticPromise])
      } else {
        await literalPromise
      }
      if (!applyIfCurrent()) return
      model.completeSearch()
      if (snapshotTrimmed !== "") {
        appendSearchKeyToHistory(term.searchKey)
      }
    })
  }

  watch(
    () =>
      [
        opts.inputSearchKey.value,
        opts.allMyNotebooksAndSubscriptions.value,
        opts.allMyCircles.value,
      ] as const,
    () => {
      if (
        opts.allMyCircles.value &&
        !oldSearchTerm.value.allMyNotebooksAndSubscriptions
      ) {
        opts.allMyNotebooksAndSubscriptions.value = true
      } else if (
        !opts.allMyNotebooksAndSubscriptions.value &&
        oldSearchTerm.value.allMyCircles
      ) {
        opts.allMyCircles.value = false
      }

      if (trimmedSearchKey.value !== "") {
        search()
      } else if (isGlobalSearch.value || opts.noteId.value) {
        fetchRecentNotes()
      }
      oldSearchTerm.value = {
        searchKey: opts.inputSearchKey.value,
        allMyNotebooksAndSubscriptions:
          opts.allMyNotebooksAndSubscriptions.value,
        allMyCircles: opts.allMyCircles.value,
      }
    },
    { immediate: true }
  )

  watch(
    () => opts.inputSearchKey.value,
    () => {
      if (opts.inputSearchKey.value.trim() === "") {
        model.clearPreviousResult()
        if (isGlobalSearch.value || opts.noteId.value) {
          model.clearRecentResult()
          fetchRecentNotes()
        }
      }
    }
  )

  watch(
    () => opts.semanticSearchEnabled.value,
    () => {
      searchGeneration.value++
      model.clearSearchCaches()
      if (trimmedSearchKey.value !== "") {
        search()
      }
    }
  )

  onBeforeUnmount(() => {
    if (timeoutId.value) {
      clearTimeout(timeoutId.value)
    }
  })

  return {
    model,
    isSearchInProgress,
    searchResult,
    filteredRecentNotes,
    recentNotesAsHits,
  }
}
