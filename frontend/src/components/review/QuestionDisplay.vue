<template>
  <div class="quiz-instruction daisy-relative daisy-mt-5" data-test="question-section">
    <QuestionStem :stem="multipleChoicesQuestion.f0__stem" />
    <QuestionChoices
      v-if="multipleChoicesQuestion.f1__choices"
      :choices="multipleChoicesQuestion.f1__choices"
      :correct-choice-index="correctChoiceIndex"
      :answer-choice-index="answer?.choiceIndex"
      :disabled="disabled"
      :question-id="questionId"
      @answer="submitAnswer($event)"
    />
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type {
  Answer,
  AnswerDto,
  MultipleChoicesQuestion,
} from "@generated/backend"
import QuestionChoices from "./QuestionChoices.vue"
import QuestionStem from "./QuestionStem.vue"

defineProps({
  multipleChoicesQuestion: {
    type: Object as PropType<MultipleChoicesQuestion>,
    required: true,
  },
  correctChoiceIndex: Number,
  answer: Object as PropType<Answer>,
  disabled: Boolean,
  questionId: {
    type: [Number, String],
    default: undefined,
  },
})

const emits = defineEmits(["answer"])

const submitAnswer = async (answerData: AnswerDto) => {
  emits("answer", answerData)
}
</script>
