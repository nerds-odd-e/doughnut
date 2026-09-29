<template>
  <div
    data-testid="book-reading-reading-control-panel"
    :data-panel-placement="panelPlacement"
    :class="wrapperClass"
    :style="wrapperStyle"
  >
    <CalloutCard :show-caret="panelPlacement === 'anchored'">
      <p class="text-sm min-w-0 flex-1 basis-full sm:basis-auto m-0">
        <span class="font-medium">{{ selectedBlockTitle }}</span>
      </p>
      <div
        class="flex flex-wrap items-center gap-2 shrink-0"
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
import { computed } from "vue"
import CalloutCard from "@/components/book-reading/CalloutCard.vue"

const props = withDefaults(
  defineProps<{
    selectedBlockTitle: string
    anchorTopPx?: number | null
    showSkimAndSkip?: boolean
  }>(),
  { anchorTopPx: null, showSkimAndSkip: true }
)

const panelPlacement = computed(() =>
  props.anchorTopPx === null ? "fixed" : "anchored"
)

const wrapperClass = computed(() => {
  const base =
    "pointer-events-none px-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] pt-2"
  return props.anchorTopPx === null ? base : `${base} absolute left-0 right-0`
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
</script>
