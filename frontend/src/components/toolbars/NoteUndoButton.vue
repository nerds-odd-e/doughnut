<template>
  <button
    class="daisy-btn daisy-btn-sm daisy-btn-ghost"
    role="button"
    :title="undoTitle"
    @click="undoDelete()"
    v-if="history"
  >
    <SvgUndo />
  </button>
  <UndoConfirmationDialog
    v-if="showDialog && history"
    :message="getUndoMessage()"
    :noteTopology="getNoteTopology()"
    :currentContent="getCurrentContent()"
    :oldContent="getOldContent()"
    :showDiff="shouldShowDiff"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  />
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import { useRouter } from "vue-router"
import SvgUndo from "../svgs/SvgUndo.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import UndoConfirmationDialog from "./UndoConfirmationDialog.vue"

const router = useRouter()
const storageAccessor = useStorageAccessor()

defineProps({
  noteId: Number,
})

const showDialog = ref(false)

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
  if (noteRealm?.note?.noteTopology?.titleOrPredicate) {
    return `"${noteRealm.note.noteTopology.titleOrPredicate}"`
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
    return noteRealm.note.noteTopology.titleOrPredicate || ""
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
      history.value.type === "edit details") &&
    history.value.textContent !== undefined
  )
})

const undoDelete = () => {
  showDialog.value = true
}

const handleConfirm = async () => {
  showDialog.value = false
  await storageAccessor.value.storedApi().undo(router)
}

const handleCancel = () => {
  showDialog.value = false
}
</script>
