<template>
  <h6>Recall Settings</h6>
  <RecallSettingForm
    v-bind="{ noteId: noteInfo.note.id, recallSetting }"
    @level-changed="$emit('levelChanged', $event)"
  />
  <h6 v-if="memoryTrackers.length">Memory Trackers</h6>
  <table v-if="memoryTrackers.length" class="daisy-table daisy-table-bordered">
    <thead>
      <tr>
        <th>Type</th>
        <th>Repetition Count</th>
        <th>Forgetting Curve Index</th>
        <th>Next Recall</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="memoryTracker in memoryTrackers" :key="memoryTracker.id">
        <NoteInfoMemoryTracker
          :model-value="memoryTracker"
          @update:model-value="updateMemoryTracker($event)"
        />
      </tr>
    </tbody>
  </table>
</template>

<script setup lang="ts">
import type { MemoryTracker, NoteInfo } from "@/generated/backend"
import { ref, computed } from "vue"
import RecallSettingForm from "../review/RecallSettingForm.vue"
import NoteInfoMemoryTracker from "./NoteInfoMemoryTracker.vue"

// Props
const props = defineProps<{
  noteInfo: NoteInfo
}>()

// Emits
defineEmits<{
  (e: "levelChanged", value: unknown): void
}>()

// Reactive state
const memoryTrackers = ref(props.noteInfo.memoryTrackers ?? [])

// Computed
const recallSetting = computed(() => props.noteInfo.recallSetting)

// Methods
const updateMemoryTracker = (newTracker: MemoryTracker) => {
  const index = memoryTrackers.value.findIndex((t) => t.id === newTracker.id)
  if (index !== -1) {
    memoryTrackers.value[index] = newTracker
  }
}
</script>

<style lang="scss" scoped>
ul {
  margin-bottom: 0;
}
</style>
