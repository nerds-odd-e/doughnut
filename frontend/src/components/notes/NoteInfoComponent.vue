<template>
  <div
    data-test="note-type-selection-dialog"
    class="daisy-mb-4"
  >
    <Select
      v-model="localNoteTypeForSelect"
      :options="noteTypeOptionsWithEmpty"
      scope-name="note"
      field="noteType"
      @update:model-value="updateNoteType"
    />
  </div>
  <NoteRecallSettingForm
    v-bind="{
      noteId: noteInfo.note.id,
      noteRecallSetting: recallSetting,
      isLinkNote: !!noteInfo.note.note.noteTopology?.targetNoteTopology,
    }"
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
import type { MemoryTracker, NoteInfo } from "@generated/backend"
import { NoteController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { ref, computed, watch } from "vue"
import { useRouter } from "vue-router"
import NoteRecallSettingForm from "../recall/NoteRecallSettingForm.vue"
import NoteInfoMemoryTracker from "./NoteInfoMemoryTracker.vue"
import Select from "../form/Select.vue"
import { noteTypeOptions } from "@/models/noteTypeOptions"
import type { NoteType } from "@/models/noteTypeOptions"

// Props
const props = defineProps<{
  noteInfo: NoteInfo
}>()

// Emits
const emit = defineEmits<{
  (e: "levelChanged", value: unknown): void
  (e: "noteTypeUpdated", noteType: NoteType): void
}>()

// Router
const router = useRouter()

// Reactive state
const memoryTrackers = ref(props.noteInfo.memoryTrackers ?? [])
const localNoteType = ref<NoteType | undefined>(props.noteInfo.noteType)

// Computed
const recallSetting = computed(() => props.noteInfo.recallSetting)
const noteTypeOptionsWithEmpty = computed(
  () => ["", ...noteTypeOptions] as string[]
)
const localNoteTypeForSelect = computed({
  get: () => localNoteType.value ?? "",
  set: (value: string) => {
    localNoteType.value = value === "" ? undefined : (value as NoteType)
  },
})

// Watch for external changes to noteInfo.noteType
watch(
  () => props.noteInfo.noteType,
  (newNoteType) => {
    localNoteType.value = newNoteType
  }
)

// Methods
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

const updateNoteType = async (newTypeString: string) => {
  const previousValue = localNoteType.value
  const newType = newTypeString === "" ? undefined : (newTypeString as NoteType)
  localNoteType.value = newType

  if (newType === props.noteInfo.noteType) {
    return
  }

  // Only send update if a type is selected (not empty/undefined)
  if (newType !== undefined) {
    const { error } = await apiCallWithLoading(() =>
      NoteController.updateNoteType({
        path: { note: props.noteInfo.note.id },
        body: newType,
      })
    )

    if (error) {
      localNoteType.value = previousValue
    } else {
      emit("noteTypeUpdated", newType)
    }
  }
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
