<template>
  <h6>Recall Settings</h6>
  <RecallSettingForm
    v-bind="{ noteId: noteInfo.note.id, recallSetting }"
    @level-changed="$emit('levelChanged', $event)"
  />
  <template v-for="memoryTracker in memoryTrackers" :key="memoryTracker.id">
    <h6>Memory Tracker</h6>
    <NoteInfoMemoryTracker
      :model-value="memoryTracker"
      @update:model-value="updateMemoryTracker($event)"
    />
  </template>
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
