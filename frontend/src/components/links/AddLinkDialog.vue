<template>
  <h3 v-if="targetNoteTopology">Complete relationship</h3>
  <SearchForNoteAndFolder
    v-if="!selectedSearchResult && !targetNoteTopology"
    v-bind="{ noteId: note?.id, notebookId: notebookId, modalCloser }"
    @selected="selectedSearchResult = $event"
    @moveUnderFolder="moveUnderFolder($event)"
  />
  <LinkInsertionChoice
    v-if="selectedSearchResult && !targetNoteTopology && note"
    :target-note-topology="selectedSearchResult.noteTopology"
    :wiki-property-option-available="wikiPropertyOptionAvailable"
    @choose-insert-wiki-link="onInsertWikiLink"
    @choose-insert-wiki-link-as-property="onInsertWikiLinkAsProperty"
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
import { ref, computed, nextTick } from "vue"
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
const { insert, canInsertWikiLinkAsProperty, insertWikiLinkAsProperty } =
  useDetailsCursorInserter()

const wikiPropertyOptionAvailable = computed(() =>
  canInsertWikiLinkAsProperty()
)

const { note, modalCloser } = defineProps<{
  note?: Note
  modalCloser?: () => void
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

async function onInsertWikiLink() {
  if (!selectedSearchResult.value) return
  const linkText = buildWikiLinkText(selectedSearchResult.value, {
    notebookId: notebookId.value,
  })
  // Close the dialog first so the editor is back in a normal DOM state
  // before Quill's insertText / setSelection are called.
  emit("closeDialog")
  await nextTick()
  insert(linkText)
}

async function onInsertWikiLinkAsProperty() {
  if (!selectedSearchResult.value) return
  const linkText = buildWikiLinkText(selectedSearchResult.value, {
    notebookId: notebookId.value,
  })
  emit("closeDialog")
  await nextTick()
  insertWikiLinkAsProperty(linkText)
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
