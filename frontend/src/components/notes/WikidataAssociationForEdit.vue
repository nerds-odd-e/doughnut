<template>
  <WikidataAssociationUnifiedDialog
    ref="dialogRef"
    :search-key="note.noteTopology.titleOrPredicate"
    :current-title="note.noteTopology.titleOrPredicate"
    :model-value="localWikidataId"
    :error-message="wikidataIdError"
    :header-title="note.wikidataId ? 'Edit Wikidata Association' : 'Associate Wikidata'"
    :show-save-button="true"
    @close="handleClose"
    @selected="handleSelected"
    @update:model-value="handleUpdate"
    @save="handleManualSave"
  />
</template>

<script setup lang="ts">
import { ref, nextTick } from "vue"
import type { PropType, ComponentPublicInstance } from "vue"
import type { Note, WikidataAssociationCreation } from "@generated/backend"
import type { StorageAccessor } from "@/store/createNoteStorage"
import useLoadingApi from "@/managedApi/useLoadingApi"
import WikidataAssociationUnifiedDialog from "./WikidataAssociationUnifiedDialog.vue"
import type { WikidataSearchEntity } from "@generated/backend"

interface WikidataError {
  body: {
    message: string
  }
}

interface WikidataIdError {
  wikidataId: string
}

const { managedApi } = useLoadingApi()
const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const emit = defineEmits(["closeDialog"])

const dialogRef = ref<ComponentPublicInstance<{
  showTitleOptionsForEntity: (entity: WikidataSearchEntity) => boolean
}> | null>(null)

const localWikidataId = ref(props.note.wikidataId || "")
const wikidataIdError = ref<string | undefined>(undefined)

const handleUpdate = (value: string) => {
  localWikidataId.value = value
  wikidataIdError.value = undefined
}

const handleSelected = async (
  entity: WikidataSearchEntity,
  _titleAction?: "replace" | "append"
) => {
  if (!entity.id) return

  // Validate the Wikidata ID
  try {
    const res = await managedApi.services.fetchWikidataEntityDataById({
      wikidataId: entity.id,
    })

    // Check if titles match (case-insensitive)
    const noteTitleUpper =
      props.note.noteTopology.titleOrPredicate.toUpperCase()
    const wikidataTitleUpper = res.WikidataTitleInEnglish.toUpperCase()

    if (
      wikidataTitleUpper === noteTitleUpper ||
      res.WikidataTitleInEnglish === ""
    ) {
      // Titles match or empty - save directly
      await saveWikidataId(entity.id)
    } else {
      // Titles differ - the unified dialog already showed Replace/Append options
      // User has chosen an action, so we save with that understanding
      // (In edit flow, we don't actually change the note title, but the user
      // has acknowledged the difference via Replace/Append choice)
      await saveWikidataId(entity.id)
    }
  } catch (e: unknown) {
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
    // Keep dialog open to show error
  }
}

const saveWikidataId = async (wikidataId: string) => {
  try {
    const associationData: WikidataAssociationCreation = {
      wikidataId,
    }
    await props.storageAccessor
      .storedApi()
      .updateWikidataId(props.note.id, associationData)
    emit("closeDialog")
  } catch (e: unknown) {
    if (typeof e === "object" && e !== null && "wikidataId" in e) {
      wikidataIdError.value = (e as WikidataIdError).wikidataId
    } else {
      wikidataIdError.value = "An unknown error occurred"
    }
    // Keep dialog open to show error
  }
}

const handleManualSave = async (wikidataId: string) => {
  // Validate the Wikidata ID
  try {
    const res = await managedApi.services.fetchWikidataEntityDataById({
      wikidataId,
    })

    // Check if titles match (case-insensitive)
    const noteTitleUpper =
      props.note.noteTopology.titleOrPredicate.toUpperCase()
    const wikidataTitleUpper = res.WikidataTitleInEnglish.toUpperCase()

    if (
      wikidataTitleUpper === noteTitleUpper ||
      res.WikidataTitleInEnglish === ""
    ) {
      // Titles match or empty - save directly
      await saveWikidataId(wikidataId)
    } else {
      // Titles differ - show Replace/Append options via unified dialog
      const entity: WikidataSearchEntity = {
        id: wikidataId,
        label: res.WikidataTitleInEnglish,
        description: "",
      }
      if (dialogRef.value?.showTitleOptionsForEntity) {
        dialogRef.value.showTitleOptionsForEntity(entity)
        await nextTick() // Ensure Vue updates the DOM
        // Don't save yet - wait for user to select Replace/Append option
        // The selected event will trigger handleSelected which will save
      } else {
        // Fallback: save anyway if dialog ref is not available
        await saveWikidataId(wikidataId)
      }
    }
  } catch (e: unknown) {
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
    // Keep dialog open to show error
  }
}

const handleClose = () => {
  emit("closeDialog")
}
</script>

