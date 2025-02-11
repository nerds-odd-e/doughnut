<template>
  <div class="quiz-instruction daisy-relative daisy-mt-5" data-test="question-section">
    <QuestionStem :stem="bareQuestion.multipleChoicesQuestion.stem" />
    <QuestionChoices
      v-if="bareQuestion.multipleChoicesQuestion.choices"
      :choices="bareQuestion.multipleChoicesQuestion.choices"
      :correct-choice-index="correctChoiceIndex"
      :answer-choice-index="answer?.choiceIndex"
      :disabled="disabled"
      @answer="submitAnswer($event)"
    />
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { Answer, AnswerDTO, BareQuestion } from "@/generated/backend"
import QuestionChoices from "./QuestionChoices.vue"
import QuestionStem from "./QuestionStem.vue"

defineProps({
  bareQuestion: {
    type: Object as PropType<BareQuestion>,
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
