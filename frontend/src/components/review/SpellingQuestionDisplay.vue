<template>
  <div class="quiz-instruction daisy-relative daisy-mt-5" data-test="question-section">
    <QuestionStem :stem="bareQuestion.multipleChoicesQuestion.stem" />
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
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { AnswerDTO, BareQuestion } from "@/generated/backend"
import TextInput from "../form/TextInput.vue"
import QuestionStem from "./QuestionStem.vue"

const props = defineProps({
  bareQuestion: {
    type: Object as PropType<BareQuestion>,
    required: true,
  },
})

const emits = defineEmits(["answer"])
const spellingAnswer = ref("")

const submitAnswer = async (answerData: AnswerDTO) => {
  emits("answer", answerData)
}
</script>
