<template>
  <h3 v-if="targetNoteTopology">Complete relationship</h3>
  <SearchForNoteAndFolder
    v-if="!targetNoteTopology"
    v-bind="{ noteId: note?.id, notebookId: notebookId }"
    @selected="targetNoteTopology = $event"
    @moveUnderFolder="moveUnderFolder($event)"
  />
  <AddRelationshipFinalize
    v-if="targetNoteTopology && note"
    v-bind="{ targetNoteTopology, note }"
    @success="$emit('closeDialog')"
    @go-back="targetNoteTopology = undefined"
  />
</template>

<script setup lang="ts">
import { ref, computed } from "vue"
import type { Note } from "@generated/doughnut-backend-api"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import AddRelationshipFinalize from "./AddRelationshipFinalize.vue"
import SearchForNoteAndFolder from "../search/SearchForNoteAndFolder.vue"
import usePopups from "../commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const { popups } = usePopups()
const storageAccessor = useStorageAccessor()

const { note } = defineProps<{
  note?: Note
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const targetNoteTopology = ref<NoteTopology | undefined>(undefined)

const noteRealm = computed(() =>
  note ? storageAccessor.value.refOfNoteRealm(note.id).value : undefined
)
const notebookId = computed(() => noteRealm.value?.notebookId)

async function moveUnderFolder(targetFolderId: number) {
  if (!(await popups.confirm("Move note into this folder?"))) {
    return
  }
  storageAccessor.value
    .storedApi()
    .moveNoteToFolder(note!.id, targetFolderId)
    .then(() => {
      emit("closeDialog")
    })
}
</script>
