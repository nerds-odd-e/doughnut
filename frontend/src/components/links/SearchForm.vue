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
    :dead-link-display-text="deadLinkPayload?.displayText"
    @choose-insert-wiki-link="onInsertWikiLink"
    @choose-insert-wiki-link-as-property="onInsertWikiLinkAsProperty"
    @choose-add-relationship="targetSearchResult = selectedSearchResult!"
    @choose-link-dead-link="onLinkDeadLinkToNote"
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
import {
  type DeadLinkPayload,
  markdownWikiTokenFromDeadLinkPayload,
} from "@/utils/wikiPropertyValueField"

const { popups } = usePopups()
const storageAccessor = useStorageAccessor()
const { insert, canInsertWikiLinkAsProperty, insertWikiLinkAsProperty } =
  useContentCursorInserter()

const wikiPropertyOptionAvailable = computed(() =>
  canInsertWikiLinkAsProperty()
)

const { note, modalCloser, deadLinkPayload } = defineProps<{
  note?: Note
  modalCloser?: () => void
  deadLinkPayload?: DeadLinkPayload
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

async function closeDialogThen(run: () => void | Promise<void>) {
  emit("closeDialog")
  await nextTick()
  await run()
}

async function onInsertWikiLink() {
  if (!selectedSearchResult.value) return
  const linkText = buildWikiLinkText(selectedSearchResult.value, {
    notebookId: notebookId.value,
  })
  await closeDialogThen(() => insert(linkText))
}

async function onInsertWikiLinkAsProperty() {
  if (!selectedSearchResult.value) return
  const linkText = buildWikiLinkText(selectedSearchResult.value, {
    notebookId: notebookId.value,
  })
  await closeDialogThen(() => insertWikiLinkAsProperty(linkText))
}

async function onLinkDeadLinkToNote() {
  if (!selectedSearchResult.value || !note || !deadLinkPayload) return
  const newLinkText = buildWikiLinkText(selectedSearchResult.value, {
    notebookId: notebookId.value,
    displayText: deadLinkPayload.displayText,
  })
  const originalToken = markdownWikiTokenFromDeadLinkPayload(deadLinkPayload)
  const currentContent =
    storageAccessor.value.refOfNoteRealm(note.id).value?.note.content ?? ""
  const newContent = currentContent.replace(originalToken, newLinkText)
  await closeDialogThen(() =>
    storageAccessor.value
      .storedApi()
      .updateTextField(note.id, "edit content", newContent)
  )
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
