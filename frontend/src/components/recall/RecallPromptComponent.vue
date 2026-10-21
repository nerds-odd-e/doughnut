<template>
  <div class="relative">
    <QuestionDisplay
      v-if="recallPrompt.mcq"
      v-bind="{
        questionStem: recallPrompt.mcq.questionStem,
        responseChoices: recallPrompt.mcq.responseChoices,
      }"
      @answer="submitAnswer($event)"
      :key="recallPrompt.id"
      :disabled="phase !== 'ready'"
      :selected-choice-index="submission?.choiceIndex"
    />

    <div v-if="phase === 'pending' || phase === 'answered'" data-test="answered-overlay" class="absolute inset-0 bg-base-100/80 z-50">
      <div v-if="phase === 'pending'" class="flex justify-center items-center h-full">
        <span class="daisy-loading daisy-loading-spinner daisy-loading-lg"></span>
      </div>
    </div>

    <RecallAnswerRecovery v-if="phase === 'uncertain'" class="mt-4" @retry="retryAnswer" />
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import type { PropType } from "vue"
import type {
  AnsweredQuestion,
  AnswerDto,
  RecallPrompt,
} from "@generated/donut-backend-api"
import { RecallPromptController } from "@generated/donut-backend-api/sdk.gen"
import { useRecallSubmission } from "@/composables/useRecallSubmission"
import QuestionDisplay from "./QuestionDisplay.vue"
import RecallAnswerRecovery from "./RecallAnswerRecovery.vue"
import { primeSoftKeyboard } from "@/utils/focusTarget"

const props = defineProps({
  recallPrompt: {
    type: Object as PropType<RecallPrompt>,
    required: true,
  },
  nextIsSpelling: {
    type: Boolean,
    default: false,
  },
})

const emits = defineEmits<{
  (e: "answered", result: AnsweredQuestion): void
}>()

const {
  phase,
  submission,
  submitAnswer: submit,
  retryAnswer,
} = useRecallSubmission<AnswerDto>(
  computed(() => props.recallPrompt.id),
  (promptId, body) => {
    if (props.nextIsSpelling) primeSoftKeyboard()
    return RecallPromptController.answer({
      path: { recallPrompt: promptId },
      body,
    })
  },
  (answer) => emits("answered", answer)
)

const submitAnswer = async (answerData: AnswerDto) => {
  if (answerData.choiceIndex === undefined) return
  await submit(answerData)
}
</script>
