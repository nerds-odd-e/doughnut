<template>
  <PopButton btn-class="btn-primary">
    <template #button_face>
      Send this question for fine tuning the question generation model
    </template>
    <template #default="{ closer }">
      <SuggestQuestionForFineTuning
        :quiz-question="questionAndAnswer.quizQuestion"
        @close-dialog="closer()"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import { PropType } from "vue"
import { QuizQuestionAndAnswer } from "@/generated/backend"
import SuggestQuestionForFineTuning from "../ai/SuggestQuestionForFineTuning.vue"

defineProps({
  questionAndAnswer: {
    type: Object as PropType<QuizQuestionAndAnswer>,
    required: true,
  },
})
</script>
