import { ref, watch, type Ref, computed } from "vue"
import type { WikidataSearchEntity } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"

export function useWikidataAssociation(
  searchKey: Ref<string> | (() => string),
  currentTitle: Ref<string> | (() => string),
  initialWikidataId?: string
) {
  const { managedApi } = useLoadingApi()

  // State
  const localWikidataId = ref(initialWikidataId || "")
  const loading = ref(false)
  const searchResults = ref<WikidataSearchEntity[]>([])
  const selectedOption = ref("")
  const selectedItem = ref<WikidataSearchEntity | null>(null)
  const showTitleOptions = ref(false)
  const titleAction = ref<"Replace" | "Append" | "">("")
  const hasSearched = ref(false)

  // Computed
  const searchKeyValue = computed(() =>
    typeof searchKey === "function" ? searchKey() : searchKey.value
  )
  const currentTitleValue = computed(() =>
    typeof currentTitle === "function" ? currentTitle() : currentTitle.value
  )

  // Methods
  const fetchSearchResults = async () => {
    const key = searchKeyValue.value
    if (!key) return
    loading.value = true
    hasSearched.value = true
    try {
      searchResults.value = await managedApi.services.searchWikidata({
        search: key,
      })
    } finally {
      loading.value = false
    }
  }

  const compareTitles = (
    current: string,
    wikidata: string
  ): "match" | "different" => {
    const currentUpper = current.toUpperCase()
    const wikidataUpper = wikidata.toUpperCase()
    return currentUpper === wikidataUpper ? "match" : "different"
  }

  const selectSearchResult = (wikidataId: string) => {
    const selected = searchResults.value.find((obj) => obj.id === wikidataId)
    if (!selected || !selected.id) return null

    selectedItem.value = selected
    localWikidataId.value = selected.id

    const comparison = compareTitles(currentTitleValue.value, selected.label)

    if (comparison === "match") {
      showTitleOptions.value = false
      return { entity: selected, needsTitleAction: false as const }
    } else {
      showTitleOptions.value = true
      return { entity: selected, needsTitleAction: true as const }
    }
  }

  const setTitleAction = (action: "Replace" | "Append" | "") => {
    titleAction.value = action
  }

  const getTitleAction = (): "replace" | "append" | undefined => {
    if (titleAction.value === "Replace") return "replace"
    if (titleAction.value === "Append") return "append"
    return undefined
  }

  const resetTitleOptions = () => {
    showTitleOptions.value = false
    titleAction.value = ""
    selectedItem.value = null
  }

  const setWikidataId = (value: string) => {
    localWikidataId.value = value
  }

  // Watch searchKey and fetch results when it changes
  watch(
    searchKeyValue,
    (newKey) => {
      if (newKey) {
        fetchSearchResults()
      }
    },
    { immediate: false }
  )

  return {
    // State
    localWikidataId,
    loading,
    searchResults,
    selectedOption,
    selectedItem,
    showTitleOptions,
    titleAction,
    hasSearched,

    // Methods
    fetchSearchResults,
    selectSearchResult,
    setTitleAction,
    getTitleAction,
    resetTitleOptions,
    setWikidataId,
    compareTitles,
  }
}
