<template>
  <h3 v-if="note">Search for a target note</h3>
  <h3 v-else>Searching</h3>
  <SearchNote
    v-if="!targetNoteTopology"
    v-bind="{ noteId: note?.id, notebookId: notebookId }"
    @selected="targetNoteTopology = $event"
    @moveUnder="moveUnder($event)"
  />
  <LinkNoteFinalize
    v-if="targetNoteTopology && note"
    v-bind="{ targetNoteTopology, note, storageAccessor }"
    @success="$emit('closeDialog')"
    @go-back="targetNoteTopology = undefined"
  />
</template>

<script setup lang="ts">
import { ref, computed } from "vue"
import type { Note } from "@generated/backend"
import type { NoteTopology } from "@generated/backend"
import LinkNoteFinalize from "./LinkNoteFinalize.vue"
import SearchNote from "../search/SearchNote.vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import usePopups from "../commons/Popups/usePopups"

const { popups } = usePopups()

const { note, storageAccessor } = defineProps<{
  note?: Note
  storageAccessor: StorageAccessor
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const targetNoteTopology = ref<NoteTopology | undefined>(undefined)

const noteRealm = computed(() =>
  note ? storageAccessor.refOfNoteRealm(note.id).value : undefined
)
const notebookId = computed(() => noteRealm.value?.notebook?.id)

async function moveUnder(targetNoteTopology: NoteTopology) {
  if (!(await popups.confirm("Move note under target note?"))) {
    return
  }
  storageAccessor
    .storedApi()
    .moveNote(note!.id, targetNoteTopology.id, {
      asFirstChild: false,
    })
    .then(() => {
      emit("closeDialog")
    })
}
</script>
