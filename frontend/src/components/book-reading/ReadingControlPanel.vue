<template>
  <div
    data-testid="book-reading-reading-control-panel"
    :data-snap-animating="isAnimating || undefined"
    class="daisy-pointer-events-none daisy-absolute daisy-bottom-0 daisy-left-0 daisy-right-0 daisy-z-20 daisy-px-2 daisy-pb-[max(0.5rem,env(safe-area-inset-bottom))] daisy-pt-2"
  >
    <div
      ref="cardRef"
      class="daisy-pointer-events-auto daisy-mx-auto daisy-max-w-3xl daisy-rounded-lg daisy-bg-base-200/95 daisy-border daisy-border-base-300 daisy-shadow-lg daisy-px-3 daisy-py-2 daisy-flex daisy-flex-wrap daisy-items-center daisy-gap-2"
    >
      <p class="daisy-text-sm daisy-min-w-0 daisy-flex-1 daisy-basis-full sm:daisy-basis-auto daisy-m-0">
        <span class="daisy-font-medium">{{ selectedBlockTitle }}</span>
      </p>
      <div
        class="daisy-flex daisy-flex-wrap daisy-items-center daisy-gap-2 daisy-shrink-0"
      >
        <button
          type="button"
          data-testid="book-reading-mark-as-read"
          class="daisy-btn daisy-btn-primary daisy-btn-sm"
          @click="emit('markAsRead')"
        >
          Read
        </button>
        <button
          type="button"
          data-testid="book-reading-mark-as-skimmed"
          class="daisy-btn daisy-btn-outline daisy-btn-sm"
          @click="emit('markAsSkimmed')"
        >
          Skim
        </button>
        <button
          type="button"
          data-testid="book-reading-mark-as-skipped"
          class="daisy-btn daisy-btn-outline daisy-btn-sm"
          @click="emit('markAsSkipped')"
        >
          Skip
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue"

const props = withDefaults(
  defineProps<{
    selectedBlockTitle: string
    snapAnimationKey?: number
  }>(),
  { snapAnimationKey: 0 }
)

const emit = defineEmits<{
  markAsRead: []
  markAsSkimmed: []
  markAsSkipped: []
}>()

const cardRef = ref<HTMLElement | null>(null)
const isAnimating = ref(false)

watch(
  () => props.snapAnimationKey,
  (key) => {
    if (key <= 0) return
    const el = cardRef.value
    if (!el) return
    el.classList.remove("snap-attention")
    requestAnimationFrame(() => {
      isAnimating.value = true
      el.classList.add("snap-attention")
      const onEnd = () => {
        el.classList.remove("snap-attention")
        isAnimating.value = false
        el.removeEventListener("animationend", onEnd)
      }
      el.addEventListener("animationend", onEnd)
    })
  }
)
</script>

<style scoped>
@keyframes snap-attention {
  0% {
    transform: scale(1);
    box-shadow: 0 0 0 0 oklch(var(--p) / 0.6);
  }
  40% {
    transform: scale(1.02);
    box-shadow: 0 0 0 8px oklch(var(--p) / 0);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 oklch(var(--p) / 0);
  }
}

.snap-attention {
  animation: snap-attention 400ms ease-out forwards;
}
</style>
