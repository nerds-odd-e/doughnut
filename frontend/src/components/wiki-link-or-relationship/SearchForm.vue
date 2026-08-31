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
import { authoredHrefLooksLikePortablePath } from "@/utils/authoredLinkMarkup"
import {
  type DeadWikiLinkPayload,
  markdownWikiTokenFromDeadWikiLinkPayload,
  pathMarkdownTokenForNote,
} from "@/utils/wikiLinkMarkup"
import {
  authoredWikiLinkTokenForInsert,
  authoredWikiLinkTokenForAmbiguousRepair,
} from "@/utils/sameNotebookWikiLinkAuthoring"
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

async function insertAuthoredWikiLink(insertFn: (linkText: string) => void) {
  if (!selectedSearchResult.value || !note) return
  const destination = selectedSearchResult.value
  const linkText = await authoredWikiLinkTokenForInsert(
    note.id,
    destination.noteTopology.id
  )
  if (linkText === undefined) return
  await closeDialogThen(() => insertFn(linkText))
}

async function onInsertWikiLink() {
  await insertAuthoredWikiLink(insert)
}

async function onInsertWikiLinkAsProperty() {
  await insertAuthoredWikiLink(insertWikiLinkAsProperty)
}

async function folderNamesForNote(noteId: number): Promise<string[]> {
  const realm = await storageAccessor.value.storedApi().loadNoteRealm(noteId)
  return (realm.ancestorFolders ?? []).map((folder) => folder.name)
}

async function wikiLinkSpellingForDestination(): Promise<string | undefined> {
  const destination = selectedSearchResult.value
  if (!destination || !note || !deadWikiLinkPayload) return
  if (deadWikiLinkPayload.resolution === "AMBIGUOUS") {
    return authoredWikiLinkTokenForAmbiguousRepair(
      note.id,
      destination.noteTopology.id,
      deadWikiLinkPayload.portablePath,
      deadWikiLinkPayload.displayText
    )
  }
  if (authoredHrefLooksLikePortablePath(deadWikiLinkPayload.portablePath)) {
    return pathMarkdownTokenForNote({
      displayText: deadWikiLinkPayload.displayText,
      folderNames: await folderNamesForNote(destination.noteTopology.id),
      title: destination.noteTopology.title,
      authoredHref: deadWikiLinkPayload.portablePath,
    })
  }
  return buildWikiLinkText(destination, {
    notebookId: notebookId.value,
    displayText: deadWikiLinkPayload.displayText,
  })
}

async function onDeadWikiLinkToNote() {
  if (!selectedSearchResult.value || !note || !deadWikiLinkPayload) return
  const originalToken =
    markdownWikiTokenFromDeadWikiLinkPayload(deadWikiLinkPayload)
  const newLinkText = await wikiLinkSpellingForDestination()
  if (newLinkText === undefined) return
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
