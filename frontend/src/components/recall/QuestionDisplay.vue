<template>
  <div class="quiz-instruction daisy-relative daisy-max-w-6xl daisy-mx-auto" data-test="question-section">
    <InactiveRecallMask :show="isActiveQuestion && isPaused" />
    <QuestionStem :stem="multipleChoicesQuestion.questionStem" />
    <QuestionChoices
      v-if="multipleChoicesQuestion.responseChoices"
      :choices="multipleChoicesQuestion.responseChoices"
      :correct-choice-index="correctChoiceIndex"
      :answer-choice-index="answer?.choiceIndex"
      :disabled="disabled"
      @answer="submitAnswer($event)"
    />
    <div
      v-if="answeredWithAiNotes"
      class="daisy-mt-6 daisy-border daisy-border-base-300 daisy-rounded-lg daisy-p-4 daisy-bg-base-200/40"
      data-test="question-ai-notes"
    >
      <p class="daisy-text-xs daisy-font-semibold daisy-opacity-60 daisy-mb-3 daisy-uppercase daisy-tracking-wide">
        Question designer notes
      </p>
      <div v-if="testedFocus?.trim()" class="daisy-space-y-1">
        <h3 class="daisy-text-sm daisy-font-semibold">Tested focus</h3>
        <p class="daisy-text-sm daisy-whitespace-pre-wrap">{{ testedFocus }}</p>
      </div>
      <div
        v-if="validationRationale?.trim()"
        :class="{ 'daisy-mt-4': testedFocus?.trim() }"
        class="daisy-space-y-1"
      >
        <h3 class="daisy-text-sm daisy-font-semibold">Why the answer fits</h3>
        <p class="daisy-text-sm daisy-whitespace-pre-wrap">{{ validationRationale }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import type { PropType } from "vue"
import type {
  Answer,
  AnswerDto,
  MultipleChoicesQuestion,
} from "@generated/doughnut-backend-api"
import InactiveRecallMask from "./InactiveRecallMask.vue"
import QuestionChoices from "./QuestionChoices.vue"
import QuestionStem from "./QuestionStem.vue"
import { useQuestionThinkingTime } from "@/composables/useQuestionThinkingTime"

const props = defineProps({
  multipleChoicesQuestion: {
    type: Object as PropType<MultipleChoicesQuestion>,
    required: true,
  },
  correctChoiceIndex: Number,
  answer: Object as PropType<Answer>,
  disabled: Boolean,
  testedFocus: String,
  validationRationale: String,
})

const emits = defineEmits(["answer"])

const isActiveQuestion = computed(() => !props.disabled && !props.answer)

const answeredWithAiNotes = computed(
  () =>
    !!props.answer &&
    !!(props.testedFocus?.trim() || props.validationRationale?.trim())
)

const { stop, isPaused } = useQuestionThinkingTime(isActiveQuestion)

const submitAnswer = async (answerData: AnswerDto) => {
  const thinkingTimeMs = stop()
  emits("answer", { ...answerData, thinkingTimeMs })
}
</script>
