<template>
  <ContainerPage>
    <main>
      <NoteWithBreadcrumb v-bind="{ note, storageAccessor }" />
    </main>
    <NoteInfoBar
      :note-id="note.id"
      :key="note.id"
      @level-changed="$emit('reloadNeeded', $event)"
    />
    <InitialReviewButtons
      :key="buttonKey"
      @do-initial-review="processForm"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import type { Note, ReviewPoint } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import usePopups from "../commons/Popups/usePopups"
import NoteInfoBar from "../notes/NoteInfoBar.vue"
import InitialReviewButtons from "./InitialReviewButtons.vue"
import NoteWithBreadcrumb from "./NoteWithBreadcrumb.vue"
import { computed } from "vue"

// Props
const { note } = defineProps<{
  note: Note
  storageAccessor: StorageAccessor
}>()

// Emits
const emit = defineEmits<{
  (e: "reloadNeeded", data: ReviewPoint): void
  (e: "initialReviewDone", data: ReviewPoint): void
}>()

// Composables
const { managedApi } = useLoadingApi()
const { popups } = usePopups()

// Computed
const buttonKey = computed(() => note.id)

// Methods
const processForm = async (skipReview: boolean) => {
  if (skipReview) {
    if (
      !(await popups.confirm(
        "Confirm to hide this note from reviewing in the future?"
      ))
    ) {
      return
    }
  }

  managedApi.restReviewsController
    .create({
      noteId: note.id,
      skipReview,
    })
    .then((data) => {
      if (skipReview) {
        emit("reloadNeeded", data)
      } else {
        emit("initialReviewDone", data)
      }
    })
}
</script>

<style>
.initial-review-paused {
  background-color: rgba(50, 50, 150, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>
