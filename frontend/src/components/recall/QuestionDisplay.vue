<template>
  <div class="quiz-instruction relative max-w-6xl mx-auto" data-test="question-section">
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
      class="mt-6 border border-base-300 rounded-lg p-4 bg-base-200/40"
      data-test="question-ai-notes"
    >
      <p class="text-xs font-semibold opacity-60 mb-3 uppercase tracking-wide">
        Question designer notes
      </p>
      <div v-if="testedFocus?.trim()" class="space-y-1">
        <h3 class="text-sm font-semibold">Tested focus</h3>
        <p class="text-sm whitespace-pre-wrap">{{ testedFocus }}</p>
      </div>
      <div
        v-if="validationRationale?.trim()"
        :class="{ 'mt-4': testedFocus?.trim() }"
        class="space-y-1"
      >
        <h3 class="text-sm font-semibold">Why the answer fits</h3>
        <p class="text-sm whitespace-pre-wrap">{{ validationRationale }}</p>
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
