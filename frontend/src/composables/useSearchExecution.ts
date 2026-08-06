import type { SearchTerm } from "@generated/doughnut-backend-api"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { debounce } from "mini-debounce"
import {
  computed,
  onBeforeUnmount,
  ref,
  shallowRef,
  watch,
  type Ref,
} from "vue"
import { executeDebouncedSearch } from "@/composables/executeDebouncedSearch"
import { SearchResultsModel } from "@/models/searchResultsModel"

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

  const recentNotesAsHits = computed(() =>
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
      const snapshotSemantic = opts.semanticSearchEnabled.value
      await executeDebouncedSearch({
        model,
        term,
        noteId: opts.noteId.value,
        notebookId: opts.notebookId.value,
        semanticEnabled: snapshotSemantic,
        isStillCurrent: () =>
          gen === searchGeneration.value &&
          snapshotTrimmed === trimmedSearchKey.value &&
          snapshotGlobal === isGlobalSearch.value &&
          snapshotSemantic === opts.semanticSearchEnabled.value,
      })
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
