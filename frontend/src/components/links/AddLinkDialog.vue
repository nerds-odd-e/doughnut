<template>
  <h3 v-if="targetNoteTopology">Complete relationship</h3>
  <SearchForNoteAndFolder
    v-if="!selectedSearchResult && !targetNoteTopology"
    v-bind="{ noteId: note?.id, notebookId: notebookId }"
    @selected="selectedSearchResult = $event"
    @moveUnderFolder="moveUnderFolder($event)"
  />
  <LinkInsertionChoice
    v-if="selectedSearchResult && !targetNoteTopology && note"
    v-bind="{ targetNoteTopology: selectedSearchResult.noteTopology }"
    @choose-insert-wiki-link="onInsertWikiLink"
    @choose-add-relationship="targetNoteTopology = selectedSearchResult!.noteTopology"
    @go-back="selectedSearchResult = undefined"
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
import type { Note, NoteSearchResult } from "@generated/doughnut-backend-api"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import AddRelationshipFinalize from "./AddRelationshipFinalize.vue"
import LinkInsertionChoice from "./LinkInsertionChoice.vue"
import SearchForNoteAndFolder from "../search/SearchForNoteAndFolder.vue"
import usePopups from "../commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { buildWikiLinkText } from "@/utils/buildWikiLinkText"
import { useDetailsCursorInserter } from "@/composables/useDetailsCursorInserter"

const { popups } = usePopups()
const storageAccessor = useStorageAccessor()
const { insert } = useDetailsCursorInserter()

const { note } = defineProps<{
  note?: Note
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const selectedSearchResult = ref<NoteSearchResult | undefined>(undefined)
const targetNoteTopology = ref<NoteTopology | undefined>(undefined)

const noteRealm = computed(() =>
  note ? storageAccessor.value.refOfNoteRealm(note.id).value : undefined
)
const notebookId = computed(() => noteRealm.value?.notebookId)

function onInsertWikiLink() {
  if (!selectedSearchResult.value) return
  const linkText = buildWikiLinkText(selectedSearchResult.value, {
    notebookId: notebookId.value,
  })
  insert(linkText)
  emit("closeDialog")
}

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
