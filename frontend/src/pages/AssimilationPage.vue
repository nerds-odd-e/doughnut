<template>
  <TeleportToHeadStatus>
    <div class="flex-grow-1">
      <span
        :class="`progress-bar ${false ? 'thin' : ''}`"
        v-if="remainingInitialReviewCountForToday !== null"
      >
        <span
          class="progress"
          :style="`width: ${(assimilatedCountOfTheDay || 0) * 100 / ((assimilatedCountOfTheDay || 0) + remainingInitialReviewCountForToday)}%`"
        >
        </span>
        <span class="progress-text">
          Assimilating: {{ assimilatedCountOfTheDay || 0 }}/{{ (assimilatedCountOfTheDay || 0) + remainingInitialReviewCountForToday }}
        </span>
      </span>
    </div>
  </TeleportToHeadStatus>
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
import TeleportToHeadStatus from "@/pages/commons/TeleportToHeadStatus.vue"
import { useAssimilationCount } from "@/composables/useAssimilationCount"

const { managedApi } = useLoadingApi()

defineProps({
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
defineEmits(["update-reviewing"])

const { setDueCount, assimilatedCountOfTheDay, incrementAssimilatedCount } =
  useAssimilationCount()

const notes = ref<Note[] | undefined>(undefined)

const note = computed(() => notes.value?.[0])
const remainingInitialReviewCountForToday = computed(
  () => notes.value?.length || 0
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
</script>

<style lang="scss" scoped>
.progress-bar {
  width: 100%;
  background-color: gray;
  height: 25px;
  border-radius: 10px;
  position: relative;

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
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
}
</style>
