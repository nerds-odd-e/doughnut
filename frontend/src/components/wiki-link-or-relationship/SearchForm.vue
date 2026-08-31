<template>
  <h3 v-if="targetSearchResult">Complete relationship</h3>
  <SearchForNoteAndFolder
    v-if="!selectedSearchResult && !targetSearchResult"
    v-bind="{
      noteId: note?.id,
      notebookId: notebookId,
      modalCloser,
      initialSearchKey: deadWikiLinkPayload?.displayText,
    }"
    @selected="selectedSearchResult = $event"
    @move-under-folder="moveUnderFolder($event)"
    @move-to-notebook-root="moveToNotebookRoot($event)"
  />
  <WikiLinkOrRelationshipChoice
    v-if="selectedSearchResult && !targetSearchResult && note"
    :target-note-topology="selectedSearchResult.noteTopology"
    :insert-wiki-link-as-property-available="insertWikiLinkAsPropertyAvailable"
    :dead-wiki-link-display-text="deadWikiLinkPayload?.displayText"
    @choose-insert-wiki-link="onInsertWikiLink"
    @choose-insert-wiki-link-as-property="onInsertWikiLinkAsProperty"
    @choose-add-relationship="targetSearchResult = selectedSearchResult!"
    @choose-dead-wiki-link="onDeadWikiLinkToNote"
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
import type { Note, NoteSearchResult } from "@generated/donut-backend-api"
import AddRelationshipFinalize from "./AddRelationshipFinalize.vue"
import WikiLinkOrRelationshipChoice from "./WikiLinkOrRelationshipChoice.vue"
import SearchForNoteAndFolder from "../search/SearchForNoteAndFolder.vue"
import usePopups from "../commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { buildWikiLinkText } from "@/utils/buildWikiLinkText"
import { useContentCursorInserter } from "@/composables/useContentCursorInserter"
import { authoredHrefLooksLikeConceptNotePath } from "@/utils/authoredLinkMarkup"
import {
  type DeadWikiLinkPayload,
  markdownWikiTokenFromDeadWikiLinkPayload,
  pathMarkdownTokenForNote,
} from "@/utils/wikiLinkMarkup"
import {
  moveBlockedBySoftDeletedTitleMessage,
  parseSoftDeletedTitleConflict,
} from "@/managedApi/softDeletedTitleConflict"

const { popups } = usePopups()
const storageAccessor = useStorageAccessor()
const { insert, canInsertWikiLinkAsProperty, insertWikiLinkAsProperty } =
  useContentCursorInserter()

const insertWikiLinkAsPropertyAvailable = computed(() =>
  canInsertWikiLinkAsProperty()
)

const { note, modalCloser, deadWikiLinkPayload } = defineProps<{
  note?: Note
  modalCloser?: () => void
  deadWikiLinkPayload?: DeadWikiLinkPayload
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

async function folderNamesForNote(noteId: number): Promise<string[]> {
  const realm = await storageAccessor.value.storedApi().loadNoteRealm(noteId)
  return (realm.ancestorFolders ?? []).map((folder) => folder.name)
}

async function onDeadWikiLinkToNote() {
  if (!selectedSearchResult.value || !note || !deadWikiLinkPayload) return
  const originalToken =
    markdownWikiTokenFromDeadWikiLinkPayload(deadWikiLinkPayload)
  const newLinkText = authoredHrefLooksLikeConceptNotePath(
    deadWikiLinkPayload.portablePath
  )
    ? pathMarkdownTokenForNote({
        displayText: deadWikiLinkPayload.displayText,
        folderNames: await folderNamesForNote(
          selectedSearchResult.value.noteTopology.id
        ),
        title: selectedSearchResult.value.noteTopology.title,
        authoredHref: deadWikiLinkPayload.portablePath,
      })
    : buildWikiLinkText(selectedSearchResult.value, {
        notebookId: notebookId.value,
        displayText: deadWikiLinkPayload.displayText,
      })
  const currentContent =
    storageAccessor.value.refOfNoteRealm(note.id).value?.note.content ?? ""
  const newContent = currentContent.replaceAll(originalToken, newLinkText)
  await closeDialogThen(() =>
    storageAccessor.value
      .storedApi()
      .updateTextField(note.id, "edit content", newContent)
  )
}

async function reportMoveBlockedBySoftDeletedTitle(e: unknown) {
  const conflict = parseSoftDeletedTitleConflict(e)
  if (!conflict) {
    throw e
  }
  await popups.confirm(moveBlockedBySoftDeletedTitleMessage(conflict))
}

async function moveUnderFolder(targetFolderId: number) {
  if (!(await popups.confirm("Move note into this folder?"))) {
    return
  }
  try {
    await storageAccessor.value
      .storedApi()
      .moveNoteToFolder(note!.id, targetFolderId)
    emit("closeDialog")
  } catch (e) {
    await reportMoveBlockedBySoftDeletedTitle(e)
  }
}

async function moveToNotebookRoot(targetNotebookId: number) {
  if (!(await popups.confirm("Move note to this notebook's root?"))) {
    return
  }
  try {
    await storageAccessor.value
      .storedApi()
      .moveNoteToNotebookRoot(note!.id, targetNotebookId)
    emit("closeDialog")
  } catch (e) {
    await reportMoveBlockedBySoftDeletedTitle(e)
  }
}
</script>
