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

const undoDelete = () => {
  storageAccessor.value.storedApi().undo(router)
}
</script>
