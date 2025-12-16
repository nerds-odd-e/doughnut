<template>
  <WikidataAssociationDialog
    ref="dialogRef"
    :search-key="note.noteTopology.title ?? ''"
    :model-value="localWikidataId"
    :error-message="wikidataIdError"
    :show-save-button="true"
    :disabled="isProcessing"
    @close="handleClose"
    @save="handleSave"
    @selected="handleSelected"
  />
</template>

<script setup lang="ts">
import { ref, watch, computed } from "vue"
import type {
  Note,
  WikidataSearchEntity,
  WikidataAssociationCreation,
} from "@generated/backend"
import WikidataAssociationDialog from "./WikidataAssociationDialog.vue"
import { WikidataController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"
import { calculateNewTitle } from "@/utils/wikidataTitleActions"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

interface WikidataIdError {
  wikidataId: string
}

const storageAccessor = useStorageAccessor()

const props = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const localWikidataId = ref(props.note.wikidataId || "")
const wikidataIdError = ref<string | undefined>(undefined)
const isSaving = ref(false)
const isProcessing = ref(false)

const saveWikidataId = async (wikidataId: string) => {
  if (isSaving.value) return
  isSaving.value = true
  try {
    const associationData: WikidataAssociationCreation = {
      wikidataId,
    }
    await storageAccessor.value
      .storedApi()
      .updateWikidataId(props.note.id, associationData)
    emit("closeDialog")
  } catch (e: unknown) {
    isSaving.value = false
    isProcessing.value = false
    if (typeof e === "object" && e !== null && "wikidataId" in e) {
      wikidataIdError.value = (e as WikidataIdError).wikidataId
    } else {
      wikidataIdError.value = "An unknown error occurred"
    }
  }
}

const handleError = (e: unknown) => {
  const errorObj = toOpenApiError(e)
  wikidataIdError.value = errorObj.message || "An unknown error occurred"
}

const dialogRef = ref<InstanceType<typeof WikidataAssociationDialog> | null>(
  null
)

const showTitleOptionsInDialog = computed(() => {
  // Access the exposed property
  if (!dialogRef.value) return false
  return (
    (dialogRef.value as { showTitleOptions?: boolean }).showTitleOptions ??
    false
  )
})

const validateAndSaveWikidataId = async (wikidataId: string) => {
  if (isProcessing.value) return
  isProcessing.value = true
  try {
    const { data: entityData, error } =
      await WikidataController.fetchWikidataEntityDataById({
        path: { wikidataId },
      })

    if (error) {
      const errorObj = toOpenApiError(error)
      wikidataIdError.value = errorObj.message || "An unknown error occurred"
      isProcessing.value = false
      return
    }

    const noteTitleUpper = (props.note.noteTopology.title ?? "").toUpperCase()
    const wikidataTitleUpper = entityData!.WikidataTitleInEnglish.toUpperCase()

    if (
      wikidataTitleUpper === noteTitleUpper ||
      entityData!.WikidataTitleInEnglish === ""
    ) {
      await saveWikidataId(wikidataId)
      // isProcessing will be reset in saveWikidataId on error, or dialog closes on success
    } else {
      // Show title options using the dialog's exposed method
      isProcessing.value = false
      const entity: WikidataSearchEntity = {
        id: wikidataId,
        label: entityData!.WikidataTitleInEnglish,
        description: "",
      }
      dialogRef.value?.showTitleOptionsForEntity(entity)
    }
  } catch (e: unknown) {
    handleError(e)
    isProcessing.value = false
  }
}

const handleSelectedForEdit = async (
  entity: WikidataSearchEntity,
  titleAction?: "replace" | "append"
) => {
  if (!entity.id || isProcessing.value) return

  isProcessing.value = true
  try {
    // Update title if titleAction is provided
    if (titleAction) {
      const currentTitle = props.note.noteTopology.title || ""
      const newTitle = calculateNewTitle(currentTitle, entity, titleAction)

      await storageAccessor.value
        .storedApi()
        .updateTextField(props.note.id, "edit title", newTitle)
    }

    // Save the Wikidata ID directly - no need to validate again since we already have the entity
    await saveWikidataId(entity.id)
  } catch (e: unknown) {
    handleError(e)
    isProcessing.value = false
  }
}

const handleSave = async (wikidataId: string) => {
  // Check if title options are currently shown (user already saw the conflict)
  // If so, save directly without re-validation
  if (dialogRef.value && showTitleOptionsInDialog.value) {
    // User clicked Save after seeing title options but without selecting action
    // Save directly without title update
    await saveWikidataId(wikidataId)
  } else {
    // First time saving, validate and save the wikidata ID
    await validateAndSaveWikidataId(wikidataId)
  }
}

const handleSelected = async (
  entity: WikidataSearchEntity,
  titleAction?: "replace" | "append"
) => {
  // When selected is called (with title action), save immediately
  await handleSelectedForEdit(entity, titleAction)
}

const handleClose = () => {
  emit("closeDialog")
}

watch(
  () => props.note.wikidataId,
  (newValue) => {
    if (newValue !== undefined) {
      localWikidataId.value = newValue
    }
  }
)
</script>

