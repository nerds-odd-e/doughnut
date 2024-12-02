<template>
    <div class="flex-grow-1">
      <div class="progress-container">
        <span
          :class="`progress-bar ${false ? 'thin' : ''}`"
          v-if="remainingInitialReviewCountForToday !== null"
          :title="`Daily Progress: ${assimilatedCountOfTheDay || 0} completed out of ${plannedForTheDay} planned for today`"
          @click="showTooltip = true"
        >
          <span
            class="progress"
            :style="`width: ${(assimilatedCountOfTheDay || 0) * 100 / plannedForTheDay}%`"
          >
          </span>
          <span class="progress-text">
            Assimilating: {{ assimilatedCountOfTheDay || 0 }}/{{ plannedForTheDay }}
          </span>
        </span>
        <span
          class="progress-bar thin"
          v-if="totalUnassimilatedCount !== undefined"
          :title="`Total Progress: ${assimilatedCountOfTheDay || 0} completed out of ${totalPlannedCount} total notes to assimilate`"
          @click="showTooltip = true"
        >
          <span
            class="progress secondary"
            :style="`width: ${(assimilatedCountOfTheDay || 0) * 100 / totalPlannedCount}%`"
          >
          </span>
          <span class="progress-text">
            Total: {{ assimilatedCountOfTheDay || 0 }}/{{ totalPlannedCount }}
          </span>
        </span>

        <!-- Popup tooltip -->
        <div v-if="showTooltip" class="tooltip-popup" @click="showTooltip = false">
          <div class="tooltip-content">
            <p>Daily Progress: {{ assimilatedCountOfTheDay || 0 }} / {{ plannedForTheDay }}</p>
            <p>Total Progress: {{ assimilatedCountOfTheDay || 0 }} / {{ totalPlannedCount }}</p>
          </div>
        </div>
      </div>
    </div>
  <ContainerPage v-bind="{ contentLoaded: notes !== undefined }">
    <div v-if="notes?.length === 0" class="text-center py-8">
      You have achieved your daily new notes goal.
    </div>
    <Assimilation
      v-if="note"
      v-bind="{ note, storageAccessor }"
      @initial-review-done="initialReviewDone"
      @reload-needed="onReloadNeeded"
      :key="note.id"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, onMounted, ref } from "vue"
import type { Note } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import timezoneParam from "@/managedApi/window/timezoneParam"
import Assimilation from "@/components/review/Assimilation.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import ContainerPage from "./commons/ContainerPage.vue"
import { useAssimilationCount } from "@/composables/useAssimilationCount"

const { managedApi } = useLoadingApi()

defineProps({
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
defineEmits(["update-reviewing"])

const {
  setDueCount,
  assimilatedCountOfTheDay,
  incrementAssimilatedCount,
  totalUnassimilatedCount,
} = useAssimilationCount()

const notes = ref<Note[] | undefined>(undefined)

const note = computed(() => notes.value?.[0])
const remainingInitialReviewCountForToday = computed(
  () => notes.value?.length || 0
)

const plannedForTheDay = computed(
  () =>
    (assimilatedCountOfTheDay.value || 0) +
    remainingInitialReviewCountForToday.value
)

const totalPlannedCount = computed(
  () =>
    (totalUnassimilatedCount.value || 0) + (assimilatedCountOfTheDay.value || 0)
)

const initialReviewDone = () => {
  incrementAssimilatedCount()
  notes.value?.shift()
  setDueCount(notes.value?.length)
}

const loadInitialReview = () => {
  managedApi.assimilationController
    .assimilating(timezoneParam())
    .then((resp) => {
      notes.value = resp
      setDueCount(resp.length)
    })
}

onMounted(() => {
  loadInitialReview()
})

const onReloadNeeded = () => {
  loadInitialReview()
}

const showTooltip = ref(false)
</script>

<style lang="scss" scoped>
.progress-container {
  position: relative;
}

.progress-bar {
  width: 100%;
  background-color: gray;
  height: 25px;
  border-radius: 10px;
  position: relative;
  cursor: help;

  &.thin {
    height: 5px;

    .progress-text {
      display: none;
    }
  }
}

.progress {
  background-color: blue;
  height: 100%;

  &.secondary {
    background-color: #4CAF50;
  }
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
}

.tooltip-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.tooltip-content {
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

  p {
    margin: 0.5rem 0;
    color: #333;
  }
}
</style>
