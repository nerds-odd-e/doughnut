<template>
  <Modal :isPopup="true" @close_request="closeDialog">
    <template #header>
      <h2>Recall Settings</h2>
    </template>
    <template #body>
      <div class="daisy-flex daisy-flex-col daisy-gap-2">
        <button
          v-if="canMoveToEnd && previousAnsweredQuestionCursor === undefined"
          class="btn large-btn"
          title="Move to end of list"
          aria-label="Move to end of list"
          @click="handleMoveToEnd"
        >
          <SvgSkip />
          <span class="daisy-ml-2">Move to end of list</span>
        </button>
        <label class="daisy-label daisy-cursor-pointer daisy-flex daisy-items-center daisy-gap-2">
          <input
            type="checkbox"
            class="daisy-toggle daisy-toggle-primary"
            :checked="treadmillMode"
            @change="handleTreadmillModeToggle"
          />
          <span class="daisy-label-text">Treadmill mode</span>
        </label>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import SvgSkip from "../svgs/SvgSkip.vue"
import Modal from "../commons/Modal.vue"
import { useRecallData } from "@/composables/useRecallData"

const props = defineProps({
  canMoveToEnd: { type: Boolean, required: true },
  previousAnsweredQuestionCursor: Number,
  currentIndex: { type: Number, required: true },
})

const emit = defineEmits<{
  (e: "close-dialog"): void
  (e: "move-to-end", index: number): void
  (e: "treadmill-mode-changed"): void
}>()

const { treadmillMode, setTreadmillMode } = useRecallData()

const closeDialog = () => {
  emit("close-dialog")
}

const handleMoveToEnd = () => {
  emit("move-to-end", props.currentIndex)
  closeDialog()
}

const handleTreadmillModeToggle = (event: Event) => {
  const target = event.target as HTMLInputElement
  setTreadmillMode(target.checked)
  emit("treadmill-mode-changed")
}
</script>

<style lang="scss" scoped>
.large-btn {
  padding: 0.75rem 1rem;
  min-height: 2.5rem;
  display: flex;
  align-items: center;
  svg {
    width: 32px;
    height: 32px;
  }
  &:disabled {
    opacity: 0.5;
  }
}
</style>
