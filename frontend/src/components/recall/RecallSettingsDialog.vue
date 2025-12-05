<template>
  <div 
    class="recall-settings-dialog daisy-bg-base-200 daisy-rounded-b-lg daisy-p-5 daisy-shadow-lg animate-dropdown"
    :style="dialogStyle"
  >
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
import { computed, onMounted, onUnmounted, watch } from "vue"
import SvgSkip from "../svgs/SvgSkip.vue"
import { useRecallData } from "@/composables/useRecallData"

const props = defineProps({
  canMoveToEnd: { type: Boolean, required: true },
  previousAnsweredQuestionCursor: Number,
  currentIndex: { type: Number, required: true },
  buttonElement: { type: Object as () => HTMLElement | null, default: null },
})

const emit = defineEmits<{
  (e: "close-dialog"): void
  (e: "move-to-end", index: number): void
  (e: "treadmill-mode-changed"): void
}>()

const { treadmillMode, setTreadmillMode } = useRecallData()

const dialogStyle = computed(() => {
  if (!props.buttonElement) {
    return {}
  }

  const rect = props.buttonElement.getBoundingClientRect()
  const spaceAbove = rect.top
  const spaceBelow = window.innerHeight - rect.bottom

  // Open upward if there's more space above, otherwise open downward
  const openUpward = spaceAbove > spaceBelow

  if (openUpward) {
    return {
      position: "fixed" as const,
      bottom: `${window.innerHeight - rect.top + 8}px`,
      right: `${window.innerWidth - rect.right}px`,
      zIndex: 1000,
      minWidth: "200px",
    }
  } else {
    return {
      position: "fixed" as const,
      top: `${rect.bottom + 8}px`,
      right: `${window.innerWidth - rect.right}px`,
      zIndex: 1000,
      minWidth: "200px",
    }
  }
})

const updatePosition = () => {
  // Force reactivity update when window is resized or scrolled
}

onMounted(() => {
  window.addEventListener("resize", updatePosition)
  window.addEventListener("scroll", updatePosition, true)
})

onUnmounted(() => {
  window.removeEventListener("resize", updatePosition)
  window.removeEventListener("scroll", updatePosition, true)
})

watch(
  () => props.buttonElement,
  () => {
    // Update position when button element changes
  },
  { immediate: true }
)

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
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-dropdown {
  animation: dropDown 0.3s ease-out;
}

.recall-settings-dialog {
  // Position is set via inline styles
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
