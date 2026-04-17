<template>
  <div
    data-testid="book-reading-reading-control-panel"
    :data-panel-placement="panelPlacement"
    :data-snap-animating="isAnimating || undefined"
    :class="wrapperClass"
    :style="wrapperStyle"
  >
    <CalloutCard ref="cardRef" :show-caret="panelPlacement === 'anchored'">
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
        <template v-if="showSkimAndSkip">
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
        </template>
      </div>
    </CalloutCard>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue"
import CalloutCard from "@/components/book-reading/CalloutCard.vue"

const props = withDefaults(
  defineProps<{
    selectedBlockTitle: string
    snapAnimationKey?: number
    anchorTopPx?: number | null
    showSkimAndSkip?: boolean
  }>(),
  { snapAnimationKey: 0, anchorTopPx: null, showSkimAndSkip: true }
)

const panelPlacement = computed(() =>
  props.anchorTopPx === null ? "fixed" : "anchored"
)

const wrapperClass = computed(() => {
  const base =
    "daisy-pointer-events-none daisy-absolute daisy-left-0 daisy-right-0 daisy-z-20 daisy-px-2 daisy-pb-[max(0.5rem,env(safe-area-inset-bottom))] daisy-pt-2"
  return props.anchorTopPx === null ? `${base} daisy-bottom-0` : base
})

const wrapperStyle = computed(() =>
  props.anchorTopPx === null
    ? undefined
    : { top: `${props.anchorTopPx}px`, bottom: "auto" }
)

const emit = defineEmits<{
  markAsRead: []
  markAsSkimmed: []
  markAsSkipped: []
}>()

const cardRef = ref<InstanceType<typeof CalloutCard> | null>(null)
const isAnimating = ref(false)

watch(
  () => props.snapAnimationKey,
  (key) => {
    if (key <= 0) return
    const el = cardRef.value?.el
    if (!el) return
    isAnimating.value = true
    el.classList.remove("snap-attention")
    const onEnd = () => {
      el.classList.remove("snap-attention")
      isAnimating.value = false
      el.removeEventListener("animationend", onEnd)
    }
    el.addEventListener("animationend", onEnd)
    requestAnimationFrame(() => {
      el.classList.add("snap-attention")
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
