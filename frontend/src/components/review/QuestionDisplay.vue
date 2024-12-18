<template>
  <div class="quiz-instruction daisy-relative daisy-mt-5" data-test="question-section">
    <div
      class="daisy-whitespace-pre-wrap"
      data-test="stem"
      v-if="bareQuestion.multipleChoicesQuestion.stem"
      v-html="stemHtml"
    ></div>

    <div
      v-if="
        !bareQuestion.multipleChoicesQuestion.choices ||
        bareQuestion.multipleChoicesQuestion.choices.length === 0
      "
    >
      <form @submit.prevent.once="submitAnswer({ spellingAnswer })">
        <TextInput
          scope-name="memory_tracker"
          field="answer"
          v-model="spellingAnswer"
          placeholder="put your answer here"
          v-focus
        />
        <input
          type="submit"
          value="Answer"
          class="daisy-btn daisy-btn-primary daisy-btn-lg daisy-w-full"
        />
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
import { ref, computed } from "vue"
import type { Answer, AnswerDTO, BareQuestion } from "@/generated/backend"
import TextInput from "../form/TextInput.vue"
import QuestionChoices from "./QuestionChoices.vue"
import markdownizer from "../form/markdownizer"

const props = defineProps({
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

const stemHtml = computed(() => {
  return props.bareQuestion.multipleChoicesQuestion.stem
    ? markdownizer.markdownToHtml(
        props.bareQuestion.multipleChoicesQuestion.stem
      )
    : ""
})

const submitAnswer = async (answerData: AnswerDTO) => {
  emits("answer", answerData)
}
</script>