<template>
  <div class="undo-confirmation">
    <div class="daisy-mb-4">
      <h2 class="daisy-text-xl daisy-font-bold daisy-mb-4">Confirm Undo</h2>
      <p>
        {{ message }}
        <NoteTitleWithLink
          v-if="noteTopology"
          :noteTopology="noteTopology"
        />?
      </p>
    </div>
    <DiffView
      v-if="showDiff"
      :current="currentContent"
      :old="oldContent"
      maxHeight="300px"
    />
    <div class="daisy-flex daisy-gap-2 daisy-mt-4 daisy-justify-end">
      <button class="daisy-btn daisy-btn-secondary" @click="handleCancel">
        Cancel
      </button>
      <button class="daisy-btn daisy-btn-success" @click="handleConfirm">
        OK
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { NoteTopology } from "@generated/backend"
import DiffView from "../commons/DiffView.vue"
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"

const props = defineProps({
  message: { type: String, required: true },
  noteTopology: { type: Object as PropType<NoteTopology>, required: false },
  currentContent: { type: String, default: "" },
  oldContent: { type: String, default: "" },
  showDiff: { type: Boolean, default: false },
  closer: { type: Function, required: true },
})

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

const handleConfirm = () => {
  emit("confirm")
  props.closer()
}

const handleCancel = () => {
  emit("cancel")
  props.closer()
}
</script>

<style scoped>
.undo-confirmation {
  /* Mobile-first: take full width of the viewport with some padding */
  width: 100%;
  max-width: 95vw;
}

/* On medium and larger screens, enforce a comfortable min width */
@media (min-width: 768px) {
  .undo-confirmation {
    min-width: 600px;
  }
}
</style>

