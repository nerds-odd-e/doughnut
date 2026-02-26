<template>
  <div v-if="isSkipped" class="daisy-alert daisy-alert-warning daisy-mb-4">
    <div class="daisy-flex daisy-items-center daisy-justify-between">
      <span>This memory tracker is currently skipped and will not appear in recall sessions.</span>
      <button
        class="daisy-btn daisy-btn-sm daisy-btn-primary"
        title="Re-enable this memory tracker"
        aria-label="Re-enable this memory tracker"
        @click="reEnable"
      >
        Re-enable
      </button>
    </div>
  </div>
  <div>
    <div v-if="memoryTracker" class="daisy-card daisy-shadow-sm daisy-mb-6">
      <div class="daisy-card-body">
        <h2 class="daisy-card-title daisy-text-lg daisy-mb-4">Memory Tracker Information</h2>
        <div class="daisy-grid daisy-grid-cols-2 daisy-gap-4 daisy-text-sm">
          <div>
            <span class="daisy-font-semibold">Assimilated Time:</span>
            <span class="daisy-ml-2">
              {{ memoryTracker.assimilatedAt ? new Date(memoryTracker.assimilatedAt).toLocaleString() : 'N/A' }}
            </span>
          </div>
          <div>
            <span class="daisy-font-semibold">Last Recall Time:</span>
            <span class="daisy-ml-2">
              {{ memoryTracker.lastRecalledAt ? new Date(memoryTracker.lastRecalledAt).toLocaleString() : 'N/A' }}
            </span>
          </div>
          <div>
            <span class="daisy-font-semibold">Next Recall Time:</span>
            <span class="daisy-ml-2">
              {{ new Date(memoryTracker.nextRecallAt).toLocaleString() }}
            </span>
          </div>
          <div>
            <span class="daisy-font-semibold">Forgetting Curve Index:</span>
            <span class="daisy-ml-2">
              {{ memoryTracker.forgettingCurveIndex ?? 'N/A' }}
            </span>
          </div>
          <div>
            <span class="daisy-font-semibold">Repetition Count:</span>
            <span class="daisy-ml-2">
              {{ memoryTracker.repetitionCount ?? 'N/A' }}
            </span>
          </div>
          <div>
            <span class="daisy-font-semibold">Spelling:</span>
            <span class="daisy-ml-2">
              {{ memoryTracker.spelling ? 'Yes' : 'No' }}
            </span>
          </div>
        </div>
      </div>
    </div>
    <div class="daisy-mb-4 daisy-flex daisy-justify-end daisy-gap-2">
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
        <SvgNoReview />
        <span>Remove from Recall</span>
      </button>
    </div>
    <div v-if="memoryTracker.note" class="daisy-mb-6">
      <NoteUnderQuestion v-bind="{ noteTopology: memoryTracker.note.noteTopology }" />
    </div>
    <div v-if="recallPrompts.length === 0" class="daisy-alert daisy-alert-info">
      No recall prompts found for this memory tracker.
    </div>
    <div v-else>
      <div
        v-for="prompt in recallPrompts"
        :key="prompt.id"
        class="daisy-card daisy-shadow-sm daisy-mb-4"
      >
        <div class="daisy-card-body">
          <div class="daisy-text-sm daisy-text-base-content/70 daisy-mb-2 daisy-flex daisy-gap-2 daisy-flex-wrap">
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
            <div v-if="prompt.answer" class="daisy-space-y-2">
              <div class="daisy-flex daisy-items-center daisy-gap-2">
                <span class="daisy-font-semibold">Your answer:</span>
                <span>{{ prompt.answer.spellingAnswer }}</span>
              </div>
              <div class="daisy-flex daisy-items-center daisy-gap-2">
                <span class="daisy-font-semibold">Result:</span>
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
            v-else-if="prompt.predefinedQuestion && prompt.answer"
            v-bind="{
              multipleChoicesQuestion: prompt.predefinedQuestion.multipleChoicesQuestion,
              correctChoiceIndex: prompt.predefinedQuestion.correctAnswerIndex,
              answer: prompt.answer,
              disabled: true,
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
import { computed } from "vue"
import type { RecallPrompt, MemoryTracker } from "@generated/backend"
import type { PropType } from "vue"
import NoteUnderQuestion from "@/components/recall/NoteUnderQuestion.vue"
import QuestionDisplay from "@/components/recall/QuestionDisplay.vue"
import ConversationButton from "@/components/recall/ConversationButton.vue"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "@/components/commons/Popups/usePopups"
import SvgNoReview from "@/components/svgs/SvgNoReview.vue"

const props = defineProps({
  recallPrompts: {
    type: Array as PropType<RecallPrompt[]>,
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
  if (
    !(await popups.confirm(`Confirm to hide this from recalls in the future?`))
  ) {
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

const reEnable = async () => {
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
