<template>
  <div class="quiz-instruction relative h-full flex flex-col" data-test="question-section">
    <InactiveRecallMask :show="isActiveQuestion && isPaused" />
    <ContentLoader v-if="loading" />
    <template v-else>
      <div class="flex-1 overflow-y-auto pb-4">
        <div v-if="recallPrompt?.spellingQuestion?.notebook" class="notebook-source mb-4">
          <NotebookLink :notebook="recallPrompt.spellingQuestion.notebook" />
        </div>
        <QuestionStem :stem="stem" />
      </div>
      <div
        v-if="overlapFeedback"
        class="daisy-alert daisy-alert-warning mb-2"
        data-testid="spelling-overlap-feedback"
      >
        {{ overlapFeedback }}
      </div>
      <RecallAnswerRecovery v-if="phase === 'uncertain'" @retry="retryAnswer" />
      <form @submit.prevent="submitAnswer" class="sticky bottom-0 bg-base-100 pt-4 pb-4">
        <TextInput
          scope-name="memory_tracker"
          field="answer"
          v-model="spellingAnswer"
          :disabled="phase !== 'ready'"
          placeholder="put your answer here"
          v-focus
        >
          <template #input_append>
            <input
              type="submit"
              value="Answer"
              class="daisy-btn daisy-btn-primary daisy-btn"
              :disabled="phase !== 'ready'"
            />
          </template>
        </TextInput>
      </form>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue"
import type {
  RecallPrompt,
  AnswerSpellingDto,
  AnsweredQuestion,
} from "@generated/donut-backend-api"
import {
  MemoryTrackerController,
  RecallPromptController,
} from "@generated/donut-backend-api/sdk.gen"
import ContentLoader from "../commons/ContentLoader.vue"
import TextInput from "../form/TextInput.vue"
import InactiveRecallMask from "./InactiveRecallMask.vue"
import QuestionStem from "./QuestionStem.vue"
import RecallAnswerRecovery from "./RecallAnswerRecovery.vue"
import { useQuestionThinkingTime } from "@/composables/useQuestionThinkingTime"
import { useRecallSubmission } from "@/composables/useRecallSubmission"
import { primeSoftKeyboard } from "@/utils/focusTarget"

const props = defineProps({
  memoryTrackerId: {
    type: Number,
    required: true,
  },
  nextIsSpelling: {
    type: Boolean,
    default: false,
  },
  overlapFeedback: {
    type: String,
    default: undefined,
  },
})

const emits = defineEmits<{ (e: "answered", result: AnsweredQuestion): void }>()
const spellingAnswer = ref("")
const recallPrompt = ref<RecallPrompt>()
const loading = ref(true)
const {
  phase,
  submitAnswer: submit,
  retryAnswer,
} = useRecallSubmission<AnswerSpellingDto>(
  computed(() => recallPrompt.value?.id),
  (promptId, body) => {
    if (props.nextIsSpelling) primeSoftKeyboard()
    return RecallPromptController.answerSpelling({
      path: { recallPrompt: promptId },
      body,
    })
  },
  (answer) => emits("answered", answer)
)

const isActiveQuestion = computed(() => !loading.value)
const { stop, isPaused, awayMs, awayCount, detourMs, detourCount, idleMs } =
  useQuestionThinkingTime(isActiveQuestion)

const stem = computed(() => recallPrompt.value?.spellingQuestion?.stem || "")

const fetchSpellingQuestion = async () => {
  loading.value = true
  const { data: prompt, error: promptError } =
    await MemoryTrackerController.getRecallPrompt({
      path: { memoryTracker: props.memoryTrackerId },
    })
  if (!promptError && prompt) {
    recallPrompt.value = prompt
  }
  loading.value = false
}

const submitAnswer = () => {
  if (phase.value !== "ready") return
  const thinkingTimeMs = stop()
  return submit({
    spellingAnswer: spellingAnswer.value,
    thinkingTimeMs,
    awayMs: Math.round(awayMs.value),
    awayCount: awayCount.value,
    detourMs: Math.round(detourMs.value),
    detourCount: detourCount.value,
    idleMs: Math.round(idleMs.value),
  })
}

onMounted(() => {
  fetchSpellingQuestion()
})
</script>
