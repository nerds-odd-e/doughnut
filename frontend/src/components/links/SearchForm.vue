<template>
  <h3 v-if="targetSearchResult">Complete relationship</h3>
  <SearchForNoteAndFolder
    v-if="!selectedSearchResult && !targetSearchResult"
    v-bind="{ noteId: note?.id, notebookId: notebookId, modalCloser }"
    @selected="selectedSearchResult = $event"
    @move-under-folder="moveUnderFolder($event)"
    @move-to-notebook-root="moveToNotebookRoot($event)"
  />
  <LinkInsertionChoice
    v-if="selectedSearchResult && !targetSearchResult && note"
    :target-note-topology="selectedSearchResult.noteTopology"
    :wiki-property-option-available="wikiPropertyOptionAvailable"
    @choose-insert-wiki-link="onInsertWikiLink"
    @choose-insert-wiki-link-as-property="onInsertWikiLinkAsProperty"
    @choose-add-relationship="targetSearchResult = selectedSearchResult!"
    @go-back="selectedSearchResult = undefined"
  />
  <AddRelationshipFinalize
    v-if="targetSearchResult && note"
    v-bind="{ targetSearchResult, note }"
    @success="$emit('closeDialog')"
    @go-back="targetSearchResult = undefined"
  />
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from "vue"
import type { Note, NoteSearchResult } from "@generated/doughnut-backend-api"
import AddRelationshipFinalize from "./AddRelationshipFinalize.vue"
import LinkInsertionChoice from "./LinkInsertionChoice.vue"
import SearchForNoteAndFolder from "../search/SearchForNoteAndFolder.vue"
import usePopups from "../commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { buildWikiLinkText } from "@/utils/buildWikiLinkText"
import { useContentCursorInserter } from "@/composables/useContentCursorInserter"

const { popups } = usePopups()
const storageAccessor = useStorageAccessor()
const { insert, canInsertWikiLinkAsProperty, insertWikiLinkAsProperty } =
  useContentCursorInserter()

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
const targetSearchResult = ref<NoteSearchResult | undefined>(undefined)

const noteRealm = computed(() =>
  note ? storageAccessor.value.refOfNoteRealm(note.id).value : undefined
)
const notebookId = computed(() => noteRealm.value?.notebookRealm.notebook.id)

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

async function moveToNotebookRoot(targetNotebookId: number) {
  if (!(await popups.confirm("Move note to this notebook's root?"))) {
    return
  }
  storageAccessor.value
    .storedApi()
    .moveNoteToNotebookRoot(note!.id, targetNotebookId)
    .then(() => {
      emit("closeDialog")
    })
}
</script>
