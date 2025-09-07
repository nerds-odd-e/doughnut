<template>
  <div class="quiz-instruction daisy-relative daisy-mt-5" data-test="question-section">
    <QuestionStem :stem="multipleChoicesQuestion.stem" />
    <QuestionChoices
      v-if="multipleChoicesQuestion.choices"
      :choices="multipleChoicesQuestion.choices"
      :correct-choice-index="correctChoiceIndex"
      :answer-choice-index="answer?.choiceIndex"
      :disabled="disabled"
      @answer="submitAnswer($event)"
    />
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type {
  Answer,
  AnswerDTO,
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
})

const emits = defineEmits(["answer"])

const submitAnswer = async (answerData: AnswerDTO) => {
  emits("answer", answerData)
}
</script>
