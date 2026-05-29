import { computed, ref } from "vue"
import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import type { WikidataSearchEntity } from "@generated/doughnut-backend-api"
import { WikidataController } from "@generated/doughnut-backend-api/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { calculateNewTitle } from "@/utils/wikidataTitleActions"
import { primeSoftKeyboard } from "@/utils/focusTarget"
import {
  validatePropertyRowsForRichEdit,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import type { Ref, ComputedRef } from "vue"

export type WikidataEditContext =
  | { type: "row"; idx: number }
  | { type: "insert" }

/** defineExpose on WikidataAssociationDialog; InstanceType<> does not include it reliably. */
type WikidataAssociationDialogExpose = {
  showTitleOptionsForEntity?: (entity: WikidataSearchEntity) => void
}

export function useWikidataPropertyDialog({
  propertyRows,
  draftKey,
  draftValue,
  searchKey,
  noteId,
  rowsAfterAdding,
  onValidationError,
  clearValidation,
  onPropertiesChanged,
  wikidataAssociationDialogRef,
}: {
  propertyRows: Ref<PropertyRow[]>
  draftKey: Ref<string>
  draftValue: Ref<string>
  searchKey: Ref<string> | ComputedRef<string>
  noteId: () => number | undefined
  rowsAfterAdding: (row: PropertyRow) => PropertyRow[]
  onValidationError: (msg: string) => void
  clearValidation: () => void
  onPropertiesChanged: (rows: PropertyRow[]) => void
  wikidataAssociationDialogRef: Ref<InstanceType<
    typeof WikidataAssociationDialog
  > | null>
}) {
  const storageAccessor = useStorageAccessor()

  const wikidataDialogOpen = ref(false)
  const wikidataEditContext = ref<WikidataEditContext | null>(null)
  const wikidataIdError = ref<string | undefined>()
  const wikidataProcessing = ref(false)
  const wikidataSavedSnapshot = ref("")

  const wikidataDialogModelValue = computed(() => {
    const c = wikidataEditContext.value
    if (!c) return ""
    if (c.type === "row") return propertyRows.value[c.idx]?.value ?? ""
    return draftValue.value
  })

  const wikidataDialogCanSaveEmptyToClear = computed(() => {
    const c = wikidataEditContext.value
    if (!c) return false
    if (c.type === "row") return !!propertyRows.value[c.idx]?.value.trim()
    return !!draftValue.value.trim()
  })

  const showTitleOptionsInDialog = computed(() => {
    const d = wikidataAssociationDialogRef.value as
      | { showTitleOptions?: boolean }
      | null
      | undefined
    return d?.showTitleOptions ?? false
  })

  function openWikidataDialog(ctx: WikidataEditContext) {
    primeSoftKeyboard()
    wikidataEditContext.value = ctx
    wikidataSavedSnapshot.value =
      ctx.type === "row"
        ? (propertyRows.value[ctx.idx]?.value.trim() ?? "")
        : draftValue.value.trim()
    wikidataIdError.value = undefined
    wikidataDialogOpen.value = true
  }

  function closeWikidataDialog() {
    wikidataDialogOpen.value = false
    wikidataEditContext.value = null
    wikidataIdError.value = undefined
    wikidataProcessing.value = false
  }

  function resetDialog() {
    wikidataDialogOpen.value = false
    wikidataEditContext.value = null
    wikidataIdError.value = undefined
    wikidataProcessing.value = false
  }

  function applyWikidataIdToRow(idx: number, wikidataId: string) {
    const rows = propertyRows.value.map((r, i) =>
      i === idx
        ? { key: r.key.trim(), value: wikidataId }
        : { key: r.key.trim(), value: r.value.trim() }
    )
    propertyRows.value = rows
    const result = validatePropertyRowsForRichEdit(propertyRows.value)
    if (!result.ok) {
      onValidationError(result.message)
      wikidataIdError.value = result.message
      return
    }
    clearValidation()
    wikidataIdError.value = undefined
    onPropertiesChanged([...propertyRows.value])
    closeWikidataDialog()
  }

  function commitInsertWithWikidataValue(trimmed: string) {
    const key = draftKey.value.trim()
    if (!key || !trimmed) return
    if (propertyRows.value.some((r) => r.key.trim() === key)) {
      wikidataIdError.value = "Duplicate property keys are not allowed."
      return
    }
    const nextRows = rowsAfterAdding({ key, value: trimmed })
    const result = validatePropertyRowsForRichEdit(nextRows)
    if (!result.ok) {
      onValidationError(result.message)
      wikidataIdError.value = result.message
      return
    }
    clearValidation()
    wikidataIdError.value = undefined
    onPropertiesChanged(nextRows)
    closeWikidataDialog()
  }

  async function applyWikidataIdAndClose(wikidataId: string) {
    const ctx = wikidataEditContext.value
    if (!ctx) return
    const trimmed = wikidataId.trim()
    if (ctx.type === "row") {
      applyWikidataIdToRow(ctx.idx, trimmed)
      return
    }
    draftValue.value = trimmed
    if (!trimmed) {
      closeWikidataDialog()
      return
    }
    commitInsertWithWikidataValue(trimmed)
  }

  async function persistWikidataIdViaValidation(wikidataId: string) {
    if (wikidataId.trim() === "") {
      await applyWikidataIdAndClose("")
      wikidataProcessing.value = false
      return
    }
    wikidataProcessing.value = true
    wikidataIdError.value = undefined
    try {
      const { data: entityData, error } =
        await WikidataController.fetchWikidataEntityDataById({
          path: { wikidataId: wikidataId.trim() },
        })
      if (error) {
        wikidataIdError.value =
          toOpenApiError(error).message || "Invalid Wikidata ID"
        wikidataProcessing.value = false
        return
      }
      const noteTitleUpper = searchKey.value.trim().toUpperCase()
      const wikidataTitleUpper =
        entityData!.WikidataTitleInEnglish.toUpperCase()
      if (
        wikidataTitleUpper === noteTitleUpper ||
        entityData!.WikidataTitleInEnglish === ""
      ) {
        await applyWikidataIdAndClose(wikidataId)
        wikidataProcessing.value = false
        return
      }
      wikidataProcessing.value = false
      const entity: WikidataSearchEntity = {
        id: wikidataId.trim(),
        label: entityData!.WikidataTitleInEnglish,
        description: "",
      }
      const expose =
        wikidataAssociationDialogRef.value as WikidataAssociationDialogExpose | null
      expose?.showTitleOptionsForEntity?.(entity)
    } catch (e: unknown) {
      wikidataIdError.value =
        toOpenApiError(e).message || "An unknown error occurred"
      wikidataProcessing.value = false
    }
  }

  async function handleWikidataSave(wikidataId: string) {
    if (wikidataProcessing.value) return
    if (wikidataId.trim() === "") {
      await persistWikidataIdViaValidation("")
      return
    }
    if (showTitleOptionsInDialog.value) {
      await applyWikidataIdAndClose(wikidataId)
      wikidataProcessing.value = false
      return
    }
    await persistWikidataIdViaValidation(wikidataId)
  }

  async function handleWikidataSelected(
    entity: WikidataSearchEntity,
    titleAction?: "replace" | "append"
  ) {
    if (!entity.id || wikidataProcessing.value) return
    wikidataProcessing.value = true
    wikidataIdError.value = undefined
    try {
      if (titleAction && noteId() != null) {
        const currentTitle = searchKey.value.trim()
        if (currentTitle) {
          const newTitle = calculateNewTitle(currentTitle, entity, titleAction)
          await storageAccessor.value
            .storedApi()
            .updateTextField(noteId()!, "edit title", newTitle)
        }
      }
      await applyWikidataIdAndClose(entity.id)
    } catch (e: unknown) {
      wikidataIdError.value =
        toOpenApiError(e).message || "An unknown error occurred"
    } finally {
      wikidataProcessing.value = false
    }
  }

  return {
    wikidataDialogOpen,
    wikidataIdError,
    wikidataProcessing,
    wikidataSavedSnapshot,
    wikidataDialogModelValue,
    wikidataDialogCanSaveEmptyToClear,
    openWikidataDialog,
    closeWikidataDialog,
    resetDialog,
    handleWikidataSave,
    handleWikidataSelected,
  }
}
