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
      <form @submit.prevent.once="submitAnswer({ spellingAnswer: answer })">
        <TextInput
          scope-name="review_point"
          field="answer"
          v-model="answer"
          placeholder="put your answer here"
          v-focus
        />
        <input type="submit" value="OK" class="btn btn-primary btn-lg btn-block" />
      </form>
    </div>
    <QuestionChoices
      v-if="bareQuestion.multipleChoicesQuestion.choices"
      :choices="bareQuestion.multipleChoicesQuestion.choices"
      :correct-choice-index="correctChoiceIndex"
      :answered-current-question="answeredCurrentQuestion"
      :answer-choice-index="answerChoiceIndex"
      :disabled="disabled"
      @answer="submitAnswer($event)"
      :assessment-current-choice-index="assessmentAnsweredIndex"
    />
  </div>
</template>

<script setup lang="ts">
import { PropType, ref } from "vue"
import { AnswerDTO, BareQuestion } from "@/generated/backend"
import TextInput from "../form/TextInput.vue"
import QuestionChoices from "./QuestionChoices.vue"

defineProps({
  bareQuestion: {
    type: Object as PropType<BareQuestion>,
    required: true,
  },
  correctChoiceIndex: Number,
  answerChoiceIndex: Number,
  disabled: Boolean,
  answeredCurrentQuestion: Boolean,
})
const emits = defineEmits(["answer"])
const answer = ref("")
const assessmentAnsweredIndex = ref(1)

const submitAnswer = async (answerData: AnswerDTO) => {
  if (typeof answerData.choiceIndex === "number") {
    assessmentAnsweredIndex.value = answerData.choiceIndex
  }

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
