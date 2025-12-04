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
</template>

<script setup lang="ts">
import { computed } from "vue"
import { useRouter } from "vue-router"
import SvgUndo from "../svgs/SvgUndo.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import usePopups from "../commons/Popups/usePopups"

const router = useRouter()
const storageAccessor = useStorageAccessor()
const { popups } = usePopups()

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

const getUndoMessage = () => {
  if (!history.value) return "Undo action"
  const actionType = history.value.type
  switch (actionType) {
    case "edit title":
      return "Are you sure you want to undo editing the title?"
    case "edit details":
      return "Are you sure you want to undo editing the details?"
    case "delete note":
      return "Are you sure you want to undo deleting the note?"
    default:
      return `Are you sure you want to undo ${actionType}?`
  }
}

const undoDelete = async () => {
  const confirmed = await popups.confirm(getUndoMessage())
  if (confirmed) {
    await storageAccessor.value.storedApi().undo(router)
  }
}
</script>
