<template>
  <GlobalBar>
    <div class="progress-container daisy-relative daisy-w-full">
      <div
        :class="`daisy-progress-bar daisy-w-full daisy-bg-gray-500 daisy-h-[25px] daisy-rounded-lg daisy-relative daisy-cursor-help ${false ? 'daisy-h-[5px]' : ''}`"
        v-if="remainingInitialReviewCountForToday !== null"
        :title="`Daily Progress: ${assimilatedCountOfTheDay || 0} completed out of ${plannedForTheDay} planned for today`"
        @click="showTooltip = true"
      >
        <div
          class="progress daisy-h-full daisy-bg-blue-500"
          :style="`width: ${(assimilatedCountOfTheDay || 0) * 100 / plannedForTheDay}%`"
        >
        </div>
        <span class="progress-text daisy-absolute daisy-top-1/2 daisy-left-1/2 daisy-transform daisy--translate-x-1/2 daisy--translate-y-1/2 daisy-text-white">
          Assimilating: {{ assimilatedCountOfTheDay || 0 }}/{{ plannedForTheDay }}
        </span>
      </div>
      <div
        class="daisy-progress-bar daisy-w-full daisy-bg-gray-500 daisy-h-[5px] daisy-rounded-lg daisy-relative daisy-cursor-help"
        v-if="totalUnassimilatedCount !== undefined"
        :title="`Total Progress: ${assimilatedCountOfTheDay || 0} completed out of ${totalPlannedCount} total notes to assimilate`"
        @click="showTooltip = true"
      >
        <span
          class="progress daisy-block daisy-h-full daisy-bg-green-500"
          :style="`width: ${(assimilatedCountOfTheDay || 0) * 100 / totalPlannedCount}%`"
        >
        </span>
        <span class="progress-text daisy-hidden">
          Total: {{ assimilatedCountOfTheDay || 0 }}/{{ totalPlannedCount }}
        </span>
      </div>

      <!-- Popup tooltip -->
      <div v-if="showTooltip" class="tooltip-popup daisy-fixed daisy-inset-0 daisy-bg-black/50 daisy-flex daisy-justify-center daisy-items-center daisy-z-[1000]" @click="showTooltip = false">
        <div class="tooltip-content daisy-bg-white daisy-p-4 daisy-rounded-lg daisy-shadow-lg">
          <p class="daisy-my-2 daisy-text-neutral">Daily Progress: {{ assimilatedCountOfTheDay || 0 }} / {{ plannedForTheDay }}</p>
          <p class="daisy-my-2 daisy-text-neutral">Total Progress: {{ assimilatedCountOfTheDay || 0 }} / {{ totalPlannedCount }}</p>
        </div>
      </div>
    </div>
  </GlobalBar>
  <div class="daisy-mx-auto daisy-min-w-0 daisy-container daisy-mt-3">
    <ContentLoader v-if="notes === undefined" />
    <template v-else>
      <div v-if="notes?.length === 0" class="daisy-text-center daisy-py-8">
        <h1 class="celebration-message daisy-text-3xl daisy-font-bold daisy-text-slate-700 daisy-my-4">
          🎉 Congratulations! You've achieved your daily assimilation goal! 🎯
        </h1>
      </div>
      <Assimilation
        v-if="note"
        v-bind="{ note }"
        @initial-review-done="initialReviewDone"
        @reload-needed="onReloadNeeded"
        :key="`${note.id}-${note.updatedAt}`"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import type { Note } from "@generated/backend"
import type { PropType } from "vue"
import Assimilation from "@/components/recall/Assimilation.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"

const props = defineProps({
  notes: {
    type: Array as PropType<Note[] | undefined>,
    required: false,
  },
  assimilatedCountOfTheDay: {
    type: Number as PropType<number | undefined>,
    required: false,
  },
  totalUnassimilatedCount: {
    type: Number as PropType<number | undefined>,
    required: false,
  },
})

const emit = defineEmits<{
  (e: "initial-review-done"): void
  (e: "reload-needed"): void
}>()

const note = computed(() => props.notes?.[0])
const remainingInitialReviewCountForToday = computed(
  () => props.notes?.length || 0
)

const plannedForTheDay = computed(
  () =>
    (props.assimilatedCountOfTheDay || 0) +
    remainingInitialReviewCountForToday.value
)

const totalPlannedCount = computed(
  () =>
    (props.totalUnassimilatedCount || 0) + (props.assimilatedCountOfTheDay || 0)
)

const initialReviewDone = () => {
  emit("initial-review-done")
}

const onReloadNeeded = () => {
  emit("reload-needed")
}

const showTooltip = ref(false)
</script>

<style lang="scss" scoped>
.progress-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
}

.celebration-message {
  font-size: 1.8rem;
  color: #2c3e50;
  font-weight: bold;
  margin: 1rem 0;
  animation: bounce 1s ease;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-20px);
  }
  60% {
    transform: translateY(-10px);
  }
}
</style>

