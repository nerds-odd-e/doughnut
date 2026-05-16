<template>
  <GlobalBar>
    <div class="progress-container relative w-full">
      <div
        :class="`daisy-progress-bar w-full bg-gray-500 h-[25px] rounded-lg relative cursor-help ${false ? 'h-[5px]' : ''}`"
        v-if="remainingAssimilationCountForToday !== null"
        :title="`Daily Progress: ${assimilatedCountOfTheDay || 0} completed out of ${plannedForTheDay} planned for today`"
        @click="showTooltip = true"
      >
        <div
          class="progress h-full bg-blue-500"
          :style="`width: ${(assimilatedCountOfTheDay || 0) * 100 / plannedForTheDay}%`"
        >
        </div>
        <span class="progress-text absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 text-white">
          Assimilating: {{ assimilatedCountOfTheDay || 0 }}/{{ plannedForTheDay }}
        </span>
      </div>
      <div
        class="daisy-progress-bar w-full bg-gray-500 h-[5px] rounded-lg relative cursor-help"
        v-if="totalUnassimilatedCount !== undefined"
        :title="`Total Progress: ${assimilatedCountOfTheDay || 0} completed out of ${totalPlannedCount} total notes to assimilate`"
        @click="showTooltip = true"
      >
        <span
          class="progress block h-full bg-green-500"
          :style="`width: ${(assimilatedCountOfTheDay || 0) * 100 / totalPlannedCount}%`"
        >
        </span>
        <span class="progress-text hidden">
          Total: {{ assimilatedCountOfTheDay || 0 }}/{{ totalPlannedCount }}
        </span>
      </div>

      <!-- Popup tooltip -->
      <div v-if="showTooltip" class="tooltip-popup fixed inset-0 bg-black/50 flex justify-center items-center z-[1000]" @click="showTooltip = false">
        <div class="tooltip-content bg-white p-4 rounded-lg shadow-lg">
          <p class="my-2 text-neutral">Daily Progress: {{ assimilatedCountOfTheDay || 0 }} / {{ plannedForTheDay }}</p>
          <p class="my-2 text-neutral">Total Progress: {{ assimilatedCountOfTheDay || 0 }} / {{ totalPlannedCount }}</p>
        </div>
      </div>
    </div>
  </GlobalBar>
  <div class="mx-auto min-w-0 container mt-3">
    <ContentLoader v-if="notes === undefined" />
    <template v-else>
      <div v-if="(notes?.length ?? 0) === 0" class="text-center py-8">
        <h1 class="celebration-message text-3xl font-bold text-slate-700 my-4">
          🎉 Congratulations! You've achieved your daily assimilation goal! 🎯
        </h1>
      </div>
      <Assimilation
        v-if="noteRealm"
        v-bind="{
          note: noteRealm.note,
          ancestorFolders: noteRealm.ancestorFolders ?? [],
        }"
        @assimilation-done="assimilationDone"
        @reload-needed="onReloadNeeded"
        :key="`${noteRealm.note.id}-${noteRealm.note.noteTopology.updatedAt}`"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import Assimilation from "@/components/recall/Assimilation.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"

const props = defineProps({
  notes: {
    type: Array as PropType<NoteRealm[] | undefined>,
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
  (e: "assimilation-done"): void
  (e: "reload-needed"): void
}>()

const noteRealm = computed(() => props.notes?.[0])
const remainingAssimilationCountForToday = computed(
  () => props.notes?.length || 0
)

const plannedForTheDay = computed(
  () =>
    (props.assimilatedCountOfTheDay || 0) +
    remainingAssimilationCountForToday.value
)

const totalPlannedCount = computed(
  () =>
    (props.totalUnassimilatedCount || 0) + (props.assimilatedCountOfTheDay || 0)
)

const assimilationDone = () => {
  emit("assimilation-done")
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

