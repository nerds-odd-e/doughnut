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
    <div v-if="recallPrompts.length === 0" class="daisy-alert daisy-alert-info">
      No recall prompts found for this memory tracker.
    </div>
    <div v-else>
      <div
        v-for="prompt in recallPrompts"
        :key="prompt.id"
        class="daisy-card shadow-sm mb-4"
      >
        <div class="daisy-card-body">
          <div class="text-sm text-base-content/70 mb-2 flex gap-2 flex-wrap">
            <span v-if="prompt.questionGeneratedTime">
              Generated: {{ new Date(prompt.questionGeneratedTime).toLocaleString() }}
            </span>
            <span v-if="prompt.isContested" class="daisy-badge daisy-badge-warning">
              Contested
            </span>
            <span v-if="prompt.answerTime">
              Answered: {{ new Date(prompt.answerTime).toLocaleString() }}
            </span>
            <span v-else>
              Unanswered
            </span>
            <span v-if="prompt.answer?.thinkingTimeMs">
              Thinking time: {{ formatThinkingTime(prompt.answer.thinkingTimeMs) }}
            </span>
          </div>
          <div v-if="prompt.questionType === 'SPELLING'">
            <div v-if="prompt.answer" class="space-y-2">
              <div class="flex items-center gap-2">
                <span class="font-semibold">Your answer:</span>
                <span>{{ prompt.answer.spellingAnswer }}</span>
              </div>
              <div class="flex items-center gap-2">
                <span class="font-semibold">Result:</span>
                <span
                  :class="{
                    'daisy-badge-success': prompt.answer.correct,
                    'daisy-badge-error': !prompt.answer.correct,
                  }"
                  class="daisy-badge"
                >
                  {{ prompt.answer.correct ? 'Correct' : 'Incorrect' }}
                </span>
              </div>
            </div>
            <div v-else class="daisy-alert daisy-alert-info">
              This is a spelling question. Details are not needed.
            </div>
          </div>
          <QuestionDisplay
            v-else-if="prompt.mcq && prompt.answer"
            v-bind="{
              multipleChoicesQuestion: prompt.mcq.multipleChoicesQuestion,
              correctChoiceIndex: prompt.mcq.correctAnswerIndex,
              answer: prompt.answer,
              disabled: true,
              testedFocus: prompt.mcq.testedFocus,
              validationRationale: prompt.mcq.validationRationale,
            }"
          />
          <QuestionDisplay
            v-else-if="prompt.multipleChoicesQuestion"
            v-bind="{
              multipleChoicesQuestion: prompt.multipleChoicesQuestion,
              disabled: true,
            }"
          />
          <ConversationButton
            v-if="prompt.answer"
            :recall-prompt-id="prompt.id"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from "vue"
import type {
  RecallPromptHistoryItem,
  MemoryTracker,
} from "@generated/doughnut-backend-api"
import NoteUnderQuestion from "@/components/recall/NoteUnderQuestion.vue"
import { recalledNoteUnderQuestionProps } from "@/components/recall/recalledNoteUnderQuestionProps"
import QuestionDisplay from "@/components/recall/QuestionDisplay.vue"
import ConversationButton from "@/components/recall/ConversationButton.vue"
import MemoryTrackerInformation from "@/components/recall/MemoryTrackerInformation.vue"
import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "@/components/commons/Popups/usePopups"
import { REMOVE_FROM_RECALL_CONFIRM } from "@/composables/useRemoveFromRecall"
import { EyeOff } from "@lucide/vue"

const props = defineProps({
  recallPrompts: {
    type: Array as PropType<RecallPromptHistoryItem[]>,
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

const formatThinkingTime = (ms: number): string => {
  if (ms < 1000) {
    return `${ms}ms`
  }
  const seconds = ms / 1000
  if (seconds < 60) {
    return `${seconds.toFixed(1)}s`
  }
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = Math.floor(seconds % 60)
  return `${minutes}m ${remainingSeconds}s`
}

const hasUnansweredPrompts = computed(() =>
  props.recallPrompts.some((prompt) => !prompt.answer && !prompt.isContested)
)

const deleteUnansweredPrompts = async () => {
  const unansweredCount = props.recallPrompts.filter(
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
  const { data: memoryTracker, error } = await apiCallWithLoading(() =>
    MemoryTrackerController.reEnable({
      path: { memoryTracker: props.memoryTrackerId },
    })
  )
  if (!error && memoryTracker) {
    emit("refresh")
  }
}
</script>
