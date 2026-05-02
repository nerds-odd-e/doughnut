<template>
  <NoteRecallSettingForm
    v-bind="{
      noteId: note.id,
      isLinkNote,
      noteRecallSetting: recallSetting,
      noteDetails: note.details,
    }"
    @level-changed="$emit('levelChanged', $event)"
    @remember-spelling-changed="$emit('rememberSpellingChanged', $event)"
  />
  <h6 v-if="memoryTrackers.length">Memory Trackers</h6>
  <table v-if="memoryTrackers.length" class="daisy-table daisy-table-bordered">
    <thead>
      <tr>
        <th>Type</th>
        <th>Recall Count</th>
        <th>Forgetting Curve Index</th>
        <th>Next Recall</th>
      </tr>
    </thead>
    <tbody>
      <tr
        v-for="memoryTracker in memoryTrackers"
        :key="memoryTracker.id"
        class="clickable-row"
        @click="navigateToMemoryTracker(memoryTracker.id)"
      >
        <NoteInfoMemoryTracker
          :model-value="memoryTracker"
          @update:model-value="updateMemoryTracker($event)"
        />
      </tr>
    </tbody>
  </table>
</template>

<script setup lang="ts">
import type {
  MemoryTracker,
  Note,
  NoteRecallInfo,
} from "@generated/doughnut-backend-api"
import { computed, ref, watch } from "vue"
import { useRouter } from "vue-router"
import { relationTypeLabelFromNoteDetails } from "@/models/relationTypeOptions"
import NoteRecallSettingForm from "../recall/NoteRecallSettingForm.vue"
import NoteInfoMemoryTracker from "./NoteInfoMemoryTracker.vue"

const props = defineProps<{
  note: Note
  noteRecallInfo: NoteRecallInfo
}>()

const emit = defineEmits<{
  (e: "levelChanged", value: unknown): void
  (e: "rememberSpellingChanged", value: boolean): void
}>()

const router = useRouter()

const memoryTrackers = ref(props.noteRecallInfo.memoryTrackers ?? [])
const recallSetting = computed(() => props.noteRecallInfo.recallSetting)

const isLinkNote = computed(
  () => relationTypeLabelFromNoteDetails(props.note.details) !== undefined
)

watch(
  () => props.noteRecallInfo.memoryTrackers,
  (next) => {
    memoryTrackers.value = next ?? []
  },
  { deep: true }
)

const updateMemoryTracker = (newTracker: MemoryTracker) => {
  const index = memoryTrackers.value.findIndex((t) => t.id === newTracker.id)
  if (index !== -1) {
    memoryTrackers.value[index] = newTracker
  }
}

const navigateToMemoryTracker = (memoryTrackerId: number) => {
  router.push({
    name: "memoryTrackerShow",
    params: { memoryTrackerId },
  })
}
</script>

<style lang="scss" scoped>
ul {
  margin-bottom: 0;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background-color: rgba(0, 0, 0, 0.05);
}
</style>
