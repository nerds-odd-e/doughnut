<template>
  <div v-if="removedFromTracking" class="daisy-alert daisy-alert-danger">
    This memory tracker has been removed from tracking.
  </div>
  <div v-else>
    <div class="daisy-mb-4 daisy-flex daisy-justify-end">
      <button
        class="daisy-btn daisy-btn-secondary"
        title="remove this note from review"
        aria-label="remove this note from review"
        @click="removeFromReview"
      >
        <SvgNoReview />
        <span>Remove from Review</span>
      </button>
    </div>
    <div v-if="recallPrompts.length === 0" class="daisy-alert daisy-alert-info">
      No recall prompts found for this memory tracker.
    </div>
    <div v-else>
      <div v-if="firstPromptNote" class="daisy-mb-6">
        <NoteUnderQuestion v-bind="{ noteTopology: firstPromptNote.noteTopology }" />
        <ViewMemoryTrackerLink :memory-tracker-id="memoryTrackerId" />
      </div>
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
          <QuestionDisplay
            v-if="prompt.predefinedQuestion && prompt.answer"
            v-bind="{
              multipleChoicesQuestion: prompt.predefinedQuestion.multipleChoicesQuestion,
              correctChoiceIndex: prompt.predefinedQuestion.correctAnswerIndex,
              answer: prompt.answer,
              disabled: true,
            }"
          />
          <QuestionDisplay
            v-else
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
import { computed, ref } from "vue"
import type { RecallPrompt } from "@generated/backend"
import type { PropType } from "vue"
import NoteUnderQuestion from "@/components/review/NoteUnderQuestion.vue"
import QuestionDisplay from "@/components/review/QuestionDisplay.vue"
import ConversationButton from "@/components/review/ConversationButton.vue"
import ViewMemoryTrackerLink from "@/components/review/ViewMemoryTrackerLink.vue"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "@/components/commons/Popups/usePopups"
import SvgNoReview from "@/components/svgs/SvgNoReview.vue"

const props = defineProps({
  recallPrompts: {
    type: Array as PropType<RecallPrompt[]>,
    required: true,
  },
  memoryTrackerId: {
    type: Number,
    required: true,
  },
})

const emit = defineEmits<{
  (e: "removedFromTracking"): void
}>()

const removedFromTracking = ref(false)
const { popups } = usePopups()

const firstPromptNote = computed(() => {
  return props.recallPrompts[0]?.note
})

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

const removeFromReview = async () => {
  if (
    !(await popups.confirm(
      `Confirm to hide this from reviewing in the future?`
    ))
  ) {
    return
  }
  const { data: memoryTracker, error } = await apiCallWithLoading(() =>
    MemoryTrackerController.removeFromRepeating({
      path: { memoryTracker: props.memoryTrackerId },
    })
  )
  if (!error && memoryTracker) {
    removedFromTracking.value = memoryTracker.removedFromTracking ?? false
    if (removedFromTracking.value) {
      emit("removedFromTracking")
    }
  }
}
</script>
