import type {
  Note,
  NoteExtractionResult,
  NoteRefinementLayoutSelectionRequestDto,
} from "@generated/doughnut-backend-api"
import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import {
  apiCallWithLoading,
  runWithBlockingApiLoading,
} from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import usePopups from "@/components/commons/Popups/usePopups"
import { computed, ref, type Ref } from "vue"
import { useRouter } from "vue-router"

export function useNoteExtractionPreview(
  note: Ref<Note>,
  layoutSelectionBody: () => NoteRefinementLayoutSelectionRequestDto
) {
  const showExtractionPreview = ref(false)
  const extractionPreview = ref<NoteExtractionResult>({
    newNoteTitle: "",
    newNoteContent: "",
    updatedOriginalNoteContent: "",
  })
  const lastAiExtractionResult = ref<NoteExtractionResult | null>(null)
  const createError = ref("")

  const canCreateExtractedNote = computed(
    () => extractionPreview.value.newNoteTitle.trim().length > 0
  )

  const { popups } = usePopups()
  const router = useRouter()
  const storageAccessor = useStorageAccessor()

  const resetExtractionPreview = () => {
    showExtractionPreview.value = false
    createError.value = ""
    lastAiExtractionResult.value = null
    extractionPreview.value = {
      newNoteTitle: "",
      newNoteContent: "",
      updatedOriginalNoteContent: "",
    }
  }

  const isExtractionPreviewEdited = () => {
    const lastResult = lastAiExtractionResult.value
    if (!lastResult) {
      return false
    }

    const current = extractionPreview.value
    return (
      current.newNoteTitle !== lastResult.newNoteTitle ||
      current.newNoteContent !== lastResult.newNoteContent ||
      current.updatedOriginalNoteContent !==
        lastResult.updatedOriginalNoteContent
    )
  }

  const runExtractionPreview = async (showPreviewOnSuccess: boolean) => {
    createError.value = ""

    const outcome = await apiCallWithLoading(
      (signal) =>
        AiController.extractNotePreview({
          path: { note: note.value.id },
          body: layoutSelectionBody(),
          signal,
        }),
      {
        blockUi: true,
        cancelable: true,
        message: "AI is generating preview...",
      }
    )

    if (outcome.status === "cancelled") {
      return
    }

    const { data, error } = outcome.result
    if (error || !data) {
      const openApiError = toOpenApiError(error)
      createError.value =
        openApiError.message ?? "Failed to generate extract preview"
      showExtractionPreview.value = true
      return
    }

    extractionPreview.value = { ...data }
    lastAiExtractionResult.value = { ...data }
    createError.value = ""
    if (showPreviewOnSuccess) {
      showExtractionPreview.value = true
    }
  }

  const openExtractionPreview = () => runExtractionPreview(true)

  const retryExtractionPreview = async () => {
    if (isExtractionPreviewEdited()) {
      const confirmed = await popups.confirm(
        "You have unsaved edits to the extract preview. Ask AI to retry will discard your edits and regenerate the preview."
      )
      if (!confirmed) {
        return
      }
    }

    await runExtractionPreview(false)
  }

  const backToLayout = () => {
    showExtractionPreview.value = false
    createError.value = ""
  }

  const createExtractedNote = async () => {
    createError.value = ""

    try {
      await runWithBlockingApiLoading(async () => {
        const response = await apiCallWithLoading(() =>
          AiController.createExtractedNote({
            path: { note: note.value.id },
            body: {
              newNoteTitle: extractionPreview.value.newNoteTitle,
              newNoteContent: extractionPreview.value.newNoteContent,
              updatedOriginalNoteContent:
                extractionPreview.value.updatedOriginalNoteContent,
            },
          })
        )

        if (response.error || !response.data) {
          const openApiError = toOpenApiError(response.error)
          createError.value =
            openApiError.message ?? "Failed to create note from preview"
          return
        }

        await storageAccessor.value
          .storedApi()
          .focusNoteRealm(router, response.data)
      }, "AI is creating note...")
    } catch (err) {
      console.error("Failed to create extracted note:", err)
      createError.value = `Error: ${err}`
    }
  }

  return {
    showExtractionPreview,
    extractionPreview,
    createError,
    canCreateExtractedNote,
    resetExtractionPreview,
    openExtractionPreview,
    retryExtractionPreview,
    backToLayout,
    createExtractedNote,
  }
}
