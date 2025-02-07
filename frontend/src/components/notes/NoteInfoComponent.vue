<template>
  <h6>Recall Settings</h6>
  <RecallSettingForm
    v-bind="{ noteId: noteInfo.note.id, recallSetting }"
    @level-changed="$emit('levelChanged', $event)"
  />
  <template v-if="memoryTracker">
    <h6>Memory Tracker</h6>
    <NoteInfoMemoryTracker
      v-model="memoryTracker"
      @update:model-value="onSelfEvaluated"
    />
  </template>
</template>

<script setup lang="ts">
import type { NoteInfo, MemoryTracker } from "@/generated/backend"
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
const memoryTracker = ref(props.noteInfo.memoryTracker)

// Computed
const recallSetting = computed(() => props.noteInfo.recallSetting)

// Methods
const onSelfEvaluated = (newMemoryTracker: MemoryTracker) => {
  memoryTracker.value = newMemoryTracker
}
</script>

<style lang="scss" scoped>
ul {
  margin-bottom: 0;
}
</style>
