import type {
  Note,
  NoteRefinementLayoutItem,
  NoteRefinementQuestionContextDto,
} from "@generated/doughnut-backend-api"
import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import {
  apiCallWithLoading,
  runWithBlockingApiLoading,
} from "@/managedApi/clientSetup"
import { useRefinementLayoutSelection } from "@/composables/useRefinementLayoutSelection"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import usePopups from "@/components/commons/Popups/usePopups"
import { onMounted, ref, type Ref } from "vue"

export function useNoteRefinementLayout(
  note: Ref<Note>,
  onContentUpdated: (newContent: string) => void,
  onLayoutReset: () => void,
  questionContext?: Ref<NoteRefinementQuestionContextDto | undefined>
) {
  const refinementLayoutItems = ref<NoteRefinementLayoutItem[]>([])
  const layoutLoadSettled = ref(false)
  const showExportExtractDialog = ref(false)
  const showExportBreakdownDialog = ref(false)

  const {
    selectedItemIds,
    isFullySelected,
    isPartiallySelected,
    setItemSelection,
    clearSelection,
    selectWhere,
  } = useRefinementLayoutSelection(refinementLayoutItems)

  const { popups } = usePopups()
  const storageAccessor = useStorageAccessor()

  const layoutSelectionBody = () => ({
    refinementLayout: { items: refinementLayoutItems.value },
    selectedItemIds: selectedItemIds.value,
  })

  const questionContextBody = () =>
    questionContext?.value !== undefined ? { body: questionContext.value } : {}

  const loadRefinementLayout = async ({
    blockUi = true,
  }: {
    blockUi?: boolean
  } = {}) => {
    const settleLayout = (items: NoteRefinementLayoutItem[]) => {
      refinementLayoutItems.value = items
      if (questionContext?.value) {
        selectWhere((item) => item.ledToQuestion)
      } else {
        clearSelection()
      }
      onLayoutReset()
      layoutLoadSettled.value = true
    }

    const settleFromResult = ({
      data,
      error,
    }: {
      data?: { items?: NoteRefinementLayoutItem[] } | null
      error?: unknown
    }) => settleLayout(!error && data?.items ? data.items : [])

    const requestLayout = (signal?: AbortSignal) =>
      AiController.generateRefinementSuggestions({
        path: { note: note.value.id },
        ...questionContextBody(),
        signal,
      })

    if (!blockUi) {
      settleFromResult(await apiCallWithLoading(requestLayout))
      return
    }

    const outcome = await apiCallWithLoading(requestLayout, {
      blockUi: true,
      message: "AI is generating refinement layout...",
      cancelable: true,
    })

    if (outcome.status === "cancelled") {
      settleLayout([])
      return
    }

    settleFromResult(outcome.result)
  }

  onMounted(() => loadRefinementLayout())

  const removeSelectedLayoutItems = async () => {
    if (selectedItemIds.value.length === 0) {
      return
    }

    const confirmed = await popups.confirm(
      `Are you sure you want to remove ${selectedItemIds.value.length} selected refinement layout item(s)? The AI will remove related content from the note.`
    )

    if (!confirmed) {
      return
    }

    await runWithBlockingApiLoading(async () => {
      const { data, error } = await apiCallWithLoading(() =>
        AiController.removeRefinementSuggestion({
          path: { note: note.value.id },
          body: layoutSelectionBody(),
        })
      )

      if (!error && data?.content !== undefined) {
        if (data.content === note.value.content) {
          return
        }

        const storedApi = storageAccessor.value?.storedApi()
        if (storedApi) {
          await storedApi.updateTextField(
            note.value.id,
            "edit content",
            data.content
          )
        }
        onContentUpdated(data.content)
        await loadRefinementLayout({ blockUi: false })
      }
    }, "AI is removing content...")
  }

  const fetchExtractRequestExport = async () => {
    const { data: response, error } = await AiController.exportExtractRequest({
      path: { note: note.value.id },
      body: layoutSelectionBody(),
    })
    if (!error && response) {
      return response
    }
    return null
  }

  const fetchBreakdownRequestExport = async () => {
    const { data: response, error } =
      await AiController.exportRefinementLayoutRequest({
        path: { note: note.value.id },
        ...questionContextBody(),
      })
    if (!error && response) {
      return response
    }
    return null
  }

  return {
    refinementLayoutItems,
    layoutLoadSettled,
    showExportExtractDialog,
    showExportBreakdownDialog,
    selectedItemIds,
    isFullySelected,
    isPartiallySelected,
    setItemSelection,
    layoutSelectionBody,
    loadRefinementLayout,
    removeSelectedLayoutItems,
    fetchExtractRequestExport,
    fetchBreakdownRequestExport,
  }
}
