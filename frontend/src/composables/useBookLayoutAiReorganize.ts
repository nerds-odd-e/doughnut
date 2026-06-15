import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type {
  BookBlockFull,
  BookLayoutReorganizationSuggestion,
  BookMutationResponseFull,
} from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, ref, toValue, type MaybeRefOrGetter } from "vue"

const BOOK_LAYOUT_SUGGEST_LOADING_MESSAGE = "Analyzing book layout…"
const BOOK_LAYOUT_APPLY_LOADING_MESSAGE = "Applying layout changes…"

export function useBookLayoutAiReorganize(
  notebookId: MaybeRefOrGetter<number>,
  bookBlocks: MaybeRefOrGetter<BookBlockFull[]>
) {
  const suggestion = ref<BookLayoutReorganizationSuggestion | null>(null)

  const previewRows = computed(() => {
    if (suggestion.value === null) return []
    const idToDepth = new Map(
      suggestion.value.blocks.map((e) => [e.id, e.depth] as const)
    )
    return toValue(bookBlocks)
      .map((block) => {
        const suggestedDepth = idToDepth.get(block.id)
        if (suggestedDepth === undefined) return null
        return {
          block,
          suggestedDepth,
          depthChanged: suggestedDepth !== block.depth,
        }
      })
      .filter((row): row is NonNullable<typeof row> => row !== null)
  })

  async function requestSuggest() {
    suggestion.value = null
    const { data, error } = await apiCallWithLoading(
      () =>
        NotebookBooksController.suggestBookLayoutReorganization({
          path: { notebook: toValue(notebookId) },
        }),
      { blockUi: true, message: BOOK_LAYOUT_SUGGEST_LOADING_MESSAGE }
    )
    if (!error && data) {
      suggestion.value = data
    }
  }

  async function confirmSuggest(): Promise<
    BookMutationResponseFull | undefined
  > {
    if (!suggestion.value) return
    const { data, error } = await apiCallWithLoading(
      () =>
        NotebookBooksController.applyBookLayoutReorganization({
          path: { notebook: toValue(notebookId) },
          body: suggestion.value!,
        }),
      { blockUi: true, message: BOOK_LAYOUT_APPLY_LOADING_MESSAGE }
    )
    if (!error && data) {
      suggestion.value = null
      return data
    }
    return
  }

  function dismiss() {
    suggestion.value = null
  }

  return {
    suggestion,
    previewRows,
    requestSuggest,
    confirmSuggest,
    dismiss,
  }
}
