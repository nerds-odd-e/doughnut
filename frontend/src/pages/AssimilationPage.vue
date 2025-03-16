<template>
      <div class="progress-container daisy:relative">
        <div
          :class="`daisy:progress-bar daisy:w-full daisy:bg-gray-500 daisy:h-[25px] daisy:rounded-lg daisy:relative daisy:cursor-help ${false ? 'daisy:h-[5px]' : ''}`"
          v-if="remainingInitialReviewCountForToday !== null"
          :title="`Daily Progress: ${assimilatedCountOfTheDay || 0} completed out of ${plannedForTheDay} planned for today`"
          @click="showTooltip = true"
        >
          <div
            class="progress daisy:h-full daisy:bg-blue-500"
            :style="`width: ${(assimilatedCountOfTheDay || 0) * 100 / plannedForTheDay}%`"
          >
          </div>
          <span class="progress-text daisy:absolute daisy:top-1/2 daisy:left-1/2 daisy:transform daisy:-translate-x-1/2 daisy:-translate-y-1/2 daisy:text-white">
            Assimilating: {{ assimilatedCountOfTheDay || 0 }}/{{ plannedForTheDay }}
          </span>
        </div>
        <div
          class="daisy:progress-bar daisy:w-full daisy:bg-gray-500 daisy:h-[5px] daisy:rounded-lg daisy:relative daisy:cursor-help"
          v-if="totalUnassimilatedCount !== undefined"
          :title="`Total Progress: ${assimilatedCountOfTheDay || 0} completed out of ${totalPlannedCount} total notes to assimilate`"
          @click="showTooltip = true"
        >
          <span
            class="progress daisy:block daisy:h-full daisy:bg-green-500"
            :style="`width: ${(assimilatedCountOfTheDay || 0) * 100 / totalPlannedCount}%`"
          >
          </span>
          <span class="progress-text daisy:hidden">
            Total: {{ assimilatedCountOfTheDay || 0 }}/{{ totalPlannedCount }}
          </span>
        </div>

        <!-- Popup tooltip -->
        <div v-if="showTooltip" class="tooltip-popup daisy:fixed daisy:inset-0 daisy:bg-black/50 daisy:flex daisy:justify-center daisy:items-center daisy:z-1000" @click="showTooltip = false">
          <div class="tooltip-content daisy:bg-white daisy:p-4 daisy:rounded-lg daisy:shadow-lg">
            <p class="daisy:my-2 daisy:text-neutral">Daily Progress: {{ assimilatedCountOfTheDay || 0 }} / {{ plannedForTheDay }}</p>
            <p class="daisy:my-2 daisy:text-neutral">Total Progress: {{ assimilatedCountOfTheDay || 0 }} / {{ totalPlannedCount }}</p>
          </div>
        </div>
      </div>
  <ContainerPage v-bind="{ contentLoaded: notes !== undefined }">
    <div v-if="notes?.length === 0" class="daisy:text-center daisy:py-8">
      <TeleportToHeadStatus>
        Assimilated {{ assimilatedCountOfTheDay }} notes today.
      </TeleportToHeadStatus>
      <h1 class="celebration-message daisy:text-3xl daisy:font-bold daisy:text-slate-700 daisy:my-4">
        🎉 Congratulations! You've achieved your daily assimilation goal! 🎯
      </h1>
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
import TeleportToHeadStatus from "@/pages/commons/TeleportToHeadStatus.vue"

const { managedApi } = useLoadingApi()

defineProps({
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
defineEmits(["update-reviewing"])

const { setDueCount, assimilatedCountOfTheDay, totalUnassimilatedCount } =
  useAssimilationCount()

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
