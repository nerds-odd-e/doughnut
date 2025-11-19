<template>
  <WikidataAssociationDialog
    ref="dialogRef"
    :search-key="note.noteTopology.titleOrPredicate"
    :model-value="localWikidataId"
    :error-message="wikidataIdError"
    :show-save-button="true"
    @close="handleClose"
    @save="handleSave"
    @selected="handleSelected"
  />
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import type {
  Note,
  WikidataSearchEntity,
  WikidataAssociationCreation,
} from "@generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"
import WikidataAssociationDialog from "./WikidataAssociationDialog.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { calculateNewTitle } from "@/utils/wikidataTitleActions"

interface WikidataError {
  body: {
    message: string
  }
}

interface WikidataIdError {
  wikidataId: string
}

const props = defineProps<{
  note: Note
  storageAccessor: StorageAccessor
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const { managedApi } = useLoadingApi()

const localWikidataId = ref(props.note.wikidataId || "")
const wikidataIdError = ref<string | undefined>(undefined)
const isSaving = ref(false)

const saveWikidataId = async (wikidataId: string) => {
  if (isSaving.value) return
  isSaving.value = true
  try {
    const associationData: WikidataAssociationCreation = {
      wikidataId,
    }
    await props.storageAccessor
      .storedApi()
      .updateWikidataId(props.note.id, associationData)
    emit("closeDialog")
  } catch (e: unknown) {
    isSaving.value = false
    if (typeof e === "object" && e !== null && "wikidataId" in e) {
      wikidataIdError.value = (e as WikidataIdError).wikidataId
    } else {
      wikidataIdError.value = "An unknown error occurred"
    }
  }
}

const handleError = (e: unknown) => {
  if (
    e instanceof Error &&
    "body" in e &&
    typeof e.body === "object" &&
    e.body &&
    "message" in e.body
  ) {
    wikidataIdError.value = (e as WikidataError).body.message
  } else {
    wikidataIdError.value = "An unknown error occurred"
  }
}

const dialogRef = ref<InstanceType<typeof WikidataAssociationDialog> | null>(
  null
)

const validateAndSaveWikidataId = async (wikidataId: string) => {
  try {
    const res = await managedApi.services.fetchWikidataEntityDataById({
      wikidataId,
    })

    const noteTitleUpper =
      props.note.noteTopology.titleOrPredicate.toUpperCase()
    const wikidataTitleUpper = res.WikidataTitleInEnglish.toUpperCase()

    if (
      wikidataTitleUpper === noteTitleUpper ||
      res.WikidataTitleInEnglish === ""
    ) {
      await saveWikidataId(wikidataId)
    } else {
      // Show title options using the dialog's exposed method
      const entity: WikidataSearchEntity = {
        id: wikidataId,
        label: res.WikidataTitleInEnglish,
        description: "",
      }
      dialogRef.value?.showTitleOptionsForEntity(entity)
    }
  } catch (e: unknown) {
    handleError(e)
  }
}

const handleSelectedForEdit = async (
  entity: WikidataSearchEntity,
  titleAction?: "replace" | "append"
) => {
  if (!entity.id) return

  try {
    await managedApi.services.fetchWikidataEntityDataById({
      wikidataId: entity.id,
    })

    // Update title if titleAction is provided
    if (titleAction) {
      const currentTitle = props.note.noteTopology.titleOrPredicate || ""
      const newTitle = calculateNewTitle(currentTitle, entity, titleAction)

      await props.storageAccessor
        .storedApi()
        .updateTextField(props.note.id, "edit title", newTitle)
    }

    await saveWikidataId(entity.id)
  } catch (e: unknown) {
    handleError(e)
  }
}

const handleSave = async (wikidataId: string) => {
  // When save is called, validate and save the wikidata ID
  await validateAndSaveWikidataId(wikidataId)
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

