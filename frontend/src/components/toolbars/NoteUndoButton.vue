<template>
  <PopButton
    v-if="history"
    :title="undoTitle"
    btn-class="daisy-btn daisy-btn-sm daisy-btn-ghost"
  >
    <template #button_face>
      <SvgUndo />
    </template>
    <template #default="{ closer }">
      <UndoConfirmationDialog
        :message="getUndoMessage()"
        :noteTopology="getNoteTopology()"
        :currentContent="getCurrentContent()"
        :oldContent="getOldContent()"
        :showDiff="shouldShowDiff"
        :closer="closer"
        @confirm="handleConfirm"
        @cancel="handleCancel"
        @discard="handleDiscard(closer)"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { useRouter } from "vue-router"
import SvgUndo from "../svgs/SvgUndo.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import UndoConfirmationDialog from "./UndoConfirmationDialog.vue"
import PopButton from "../commons/Popups/PopButton.vue"

const router = useRouter()
const storageAccessor = useStorageAccessor()

defineProps({
  noteId: Number,
})

const history = computed(() => storageAccessor.value.peekUndo())
const undoTitle = computed(() => {
  if (history.value) {
    return `undo ${history.value.type}`
  }
  return "undo"
})

const getNoteTopology = () => {
  if (!history.value) return undefined
  const noteRealm = storageAccessor.value.refOfNoteRealm(
    history.value.noteId
  ).value
  return noteRealm?.note?.noteTopology
}

const getNoteIdentifier = (noteId: Doughnut.ID): string => {
  const noteRealm = storageAccessor.value.refOfNoteRealm(noteId).value
  if (noteRealm?.note?.noteTopology?.title) {
    return `"${noteRealm.note.noteTopology.title}"`
  }
  return `note id: ${noteId}`
}

const getCurrentContent = (): string => {
  if (!history.value) return ""
  const noteRealm = storageAccessor.value.refOfNoteRealm(
    history.value.noteId
  ).value
  if (!noteRealm) return ""

  if (history.value.type === "edit title") {
    return noteRealm.note.noteTopology.title || ""
  }
  if (history.value.type === "edit details") {
    return noteRealm.note.details || ""
  }
  return ""
}

const getOldContent = (): string => {
  if (!history.value?.textContent) return ""
  return history.value.textContent
}

const getUndoMessage = (): string => {
  if (!history.value) return "Undo action"
  const actionType = history.value.type
  const noteTopology = getNoteTopology()
  const noteIdentifier = noteTopology
    ? ""
    : getNoteIdentifier(history.value.noteId)
  switch (actionType) {
    case "edit title":
      return noteTopology
        ? "Are you sure you want to undo editing the title of "
        : `Are you sure you want to undo editing the title of ${noteIdentifier}?`
    case "edit details":
      return noteTopology
        ? "Are you sure you want to undo editing the details of "
        : `Are you sure you want to undo editing the details of ${noteIdentifier}?`
    case "delete note":
      return noteTopology
        ? "Are you sure you want to undo deleting "
        : `Are you sure you want to undo deleting ${noteIdentifier}?`
    default:
      return noteTopology
        ? `Are you sure you want to undo ${actionType} for `
        : `Are you sure you want to undo ${actionType} for ${noteIdentifier}?`
  }
}

const shouldShowDiff = computed((): boolean => {
  return !!(
    history.value &&
    (history.value.type === "edit title" ||
      history.value.type === "edit details")
  )
})

const handleConfirm = async () => {
  await storageAccessor.value.storedApi().undo(router)
}

const handleCancel = () => {
  // PopButton will handle closing the dialog
}

const handleDiscard = (closer: () => void) => {
  storageAccessor.value.discardUndo()
  if (!storageAccessor.value.peekUndo()) {
    closer()
  }
}
</script>
