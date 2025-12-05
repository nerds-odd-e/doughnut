<template>
  <div class="recall-settings-dialog daisy-bg-base-200 daisy-rounded-b-lg daisy-p-5 daisy-shadow-lg animate-dropdown daisy-relative">
    <button 
      class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-btn-circle daisy-absolute daisy-top-2 daisy-right-2" 
      @click="closeDialog" 
      title="Close"
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
        <path d="M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"/>
      </svg>
    </button>
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
  </div>
</template>

<script setup lang="ts">
import SvgSkip from "../svgs/SvgSkip.vue"
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
@keyframes dropDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-dropdown {
  animation: dropDown 0.3s ease-out;
  transform-origin: top;
}

.recall-settings-dialog {
  position: absolute;
  top: 100%;
  right: 0;
  z-index: 1000;
  min-width: 200px;
  margin-top: 0.5rem;
}

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
