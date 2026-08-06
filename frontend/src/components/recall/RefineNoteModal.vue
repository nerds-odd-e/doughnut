<template>
  <Teleport to="body">
    <dialog
      ref="dialogRef"
      class="daisy-modal"
      :class="{ 'daisy-modal-open': open }"
      data-test="refine-note-modal"
      @close="close"
    >
      <div class="daisy-modal-box max-w-4xl max-h-[90vh] overflow-y-auto">
        <h3 v-if="open" class="font-bold text-lg mb-3">Refine note</h3>
        <NoteRefinement
          v-if="open"
          :key="note.id"
          :note="note"
          @content-updated="emit('contentUpdated')"
        />
        <div v-if="open" class="daisy-modal-action mt-4">
          <button
            type="button"
            class="daisy-btn"
            data-test="close-refine-note-modal"
            @click="close"
          >
            Close
          </button>
        </div>
      </div>
      <form method="dialog" class="daisy-modal-backdrop">
        <button type="button" @click="close">close</button>
      </form>
    </dialog>
  </Teleport>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import { useDaisyDialog } from "@/composables/useDaisyDialog"
import { ref, toRef, watch } from "vue"
import NoteRefinement from "./NoteRefinement.vue"

const props = defineProps<{
  open: boolean
  note: Note
}>()

const emit = defineEmits<{
  "update:open": [value: boolean]
  contentUpdated: []
}>()

const dialogRef = ref<HTMLDialogElement | null>(null)
useDaisyDialog(toRef(props, "open"), dialogRef)

watch(
  () => props.note.id,
  () => {
    emit("update:open", false)
  }
)

const close = () => {
  emit("update:open", false)
}
</script>
