<template>
  <div class="quiz-instruction" data-test="question-section">
    <div
      style="white-space: pre-wrap"
      data-test="stem"
      v-if="bareQuestion.multipleChoicesQuestion.stem"
      v-html="bareQuestion.multipleChoicesQuestion.stem"
    ></div>

    <div
      v-if="
        !bareQuestion.multipleChoicesQuestion.choices ||
        bareQuestion.multipleChoicesQuestion.choices.length === 0
      "
    >
      <form @submit.prevent.once="submitAnswer({ spellingAnswer })">
        <TextInput
          scope-name="review_point"
          field="answer"
          v-model="spellingAnswer"
          placeholder="put your answer here"
          v-focus
        />
        <input type="submit" value="Answer" class="btn btn-primary btn-lg btn-block" />
      </form>
    </div>
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
import { ref } from "vue"
import type { Answer, AnswerDTO, BareQuestion } from "@/generated/backend"
import TextInput from "../form/TextInput.vue"
import QuestionChoices from "./QuestionChoices.vue"

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
const spellingAnswer = ref("")

const submitAnswer = async (answerData: AnswerDTO) => {
  emits("answer", answerData)
}
</script>

<style lang="scss" scoped>
.quiz-instruction {
  position: relative;
  margin-top: 20px;
}

.mark-question {
  button {
    border: none;
    background: none;
  }
}
</style>
