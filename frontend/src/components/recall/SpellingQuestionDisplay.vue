<template>
  <div class="quiz-instruction relative h-full flex flex-col" data-test="question-section">
    <InactiveRecallMask :show="isActiveQuestion && isPaused" />
      <div class="flex-1 overflow-y-auto pb-4">
        <div v-if="recallPrompt?.spellingQuestion?.notebook" class="notebook-source mb-4">
          <NotebookLink :notebook="recallPrompt.spellingQuestion.notebook" />
        </div>
        <QuestionStem :stem="stem" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue"
import type {
  RecallPrompt,
  AnswerSpellingDto,
  AnsweredQuestion,
} from "@generated/donut-backend-api"
import { RecallPromptController } from "@generated/donut-backend-api/sdk.gen"
import TextInput from "../form/TextInput.vue"
import InactiveRecallMask from "./InactiveRecallMask.vue"
import QuestionStem from "./QuestionStem.vue"
import RecallAnswerRecovery from "./RecallAnswerRecovery.vue"
import { useQuestionThinkingTime } from "@/composables/useQuestionThinkingTime"
import { useRecallSubmission } from "@/composables/useRecallSubmission"
import { primeSoftKeyboard } from "@/utils/focusTarget"

const props = defineProps<{
  recallPrompt: RecallPrompt
  nextIsSpelling?: boolean
}>()

const emits = defineEmits<{ (e: "answered", result: AnsweredQuestion): void }>()
const spellingAnswer = ref("")
const {
  phase,
  submitAnswer: submit,
  retryAnswer,
} = useRecallSubmission<AnswerSpellingDto>(
  computed(() => props.recallPrompt.id),
  (promptId, body) => {
    if (props.nextIsSpelling) primeSoftKeyboard()
    return RecallPromptController.answerSpelling({
      path: { recallPrompt: promptId },
      body,
    })
  },
  (answer) => emits("answered", answer)
)

const isActiveQuestion = computed(() => !!props.recallPrompt)
const { stop, isPaused, awayMs, awayCount, detourMs, detourCount, idleMs } =
  useQuestionThinkingTime(isActiveQuestion)

const stem = computed(() => props.recallPrompt.spellingQuestion?.stem || "")

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
</script>
