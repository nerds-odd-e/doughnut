<template>
  <div v-if="isSkipped" class="daisy-alert daisy-alert-warning mb-4">
    <div class="flex items-center justify-between">
      <span>This memory tracker is currently skipped and will not appear in recall sessions.</span>
      <button
        class="daisy-btn daisy-btn-sm daisy-btn-primary"
        title="Revive this memory tracker"
        aria-label="Revive this memory tracker"
        @click="revive"
      >
        Revive
      </button>
    </div>
  </div>
  <div>
    <MemoryTrackerInformation
      v-if="memoryTracker"
      :memory-tracker="memoryTracker"
    />
    <div class="mb-4 flex justify-end gap-2">
      <button
        v-if="hasUnansweredPrompts"
        class="daisy-btn daisy-btn-error"
        title="delete all unanswered recall prompts"
        aria-label="delete all unanswered recall prompts"
        @click="deleteUnansweredPrompts"
      >
        <span>Delete Unanswered Prompts</span>
      </button>
      <button
        v-if="!isSkipped"
        class="daisy-btn daisy-btn-secondary"
        title="remove this note from recall"
        aria-label="remove this note from recall"
        @click="removeFromRecall"
      >
        <EyeOff class="w-6 h-6" />
        <span>Remove from Recall</span>
      </button>
    </div>
    <div v-if="memoryTracker.recalledNote" class="mb-6">
      <NoteUnderQuestion
        v-bind="recalledNoteUnderQuestionProps(memoryTracker.recalledNote)"
      />
    </div>
    <RecallHistory :items="recallHistory" />
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from "vue"
import type {
  RecallHistoryItem,
  MemoryTracker,
} from "@generated/donut-backend-api"
import NoteUnderQuestion from "@/components/recall/NoteUnderQuestion.vue"
import { recalledNoteUnderQuestionProps } from "@/components/recall/recalledNoteUnderQuestionProps"
import MemoryTrackerInformation from "@/components/recall/MemoryTrackerInformation.vue"
import RecallHistory from "@/components/recall/RecallHistory.vue"
import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "@/components/commons/Popups/usePopups"
import { REMOVE_FROM_RECALL_CONFIRM } from "@/composables/useRemoveFromRecall"
import { EyeOff } from "@lucide/vue"

const props = defineProps({
  recallHistory: {
    type: Array as PropType<RecallHistoryItem[]>,
    required: true,
  },
  memoryTracker: {
    type: Object as PropType<MemoryTracker>,
    required: true,
  },
  memoryTrackerId: {
    type: Number,
    required: true,
  },
})

const emit = defineEmits<{
  (e: "removedFromTracking"): void
  (e: "refresh"): void
}>()

const { popups } = usePopups()

const isSkipped = computed(
  () => props.memoryTracker.removedFromTracking === true
)

const historyPrompts = computed(() =>
  props.recallHistory.flatMap((item) =>
    item.recallPrompt ? [item.recallPrompt] : []
  )
)

const hasUnansweredPrompts = computed(() =>
  historyPrompts.value.some((prompt) => !prompt.answer && !prompt.isContested)
)

const deleteUnansweredPrompts = async () => {
  const unansweredCount = historyPrompts.value.filter(
    (p) => !p.answer && !p.isContested
  ).length
  if (
    !(await popups.confirm(
      `Are you sure you want to delete ${unansweredCount} unanswered recall prompt${unansweredCount !== 1 ? "s" : ""}?`
    ))
  ) {
    return
  }
  const { error } = await apiCallWithLoading(() =>
    MemoryTrackerController.deleteUnansweredRecallPrompts({
      path: { memoryTracker: props.memoryTrackerId },
    })
  )
  if (!error) {
    emit("refresh")
  }
}

const removeFromRecall = async () => {
  if (!(await popups.confirm(REMOVE_FROM_RECALL_CONFIRM))) {
    return
  }
  const { data: memoryTracker, error } = await apiCallWithLoading(() =>
    MemoryTrackerController.removeFromRepeating({
      path: { memoryTracker: props.memoryTrackerId },
    })
  )
  if (!error && memoryTracker) {
    if (memoryTracker.removedFromTracking) {
      emit("removedFromTracking")
    }
    emit("refresh")
  }
}

const revive = async () => {
  const { error } = await apiCallWithLoading(() =>
    MemoryTrackerController.reEnable({
      path: { memoryTracker: props.memoryTrackerId },
    })
  )
  if (!error) {
    emit("refresh")
  }
}
</script>
