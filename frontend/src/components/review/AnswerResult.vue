<template>
  <div class="alert alert-success" v-if="answeredQuestion.answer.correct">
    Correct!
  </div>
  <div class="alert alert-danger" v-else>
    <strong>
      {{ "Your answer `" + answeredQuestion.answerDisplay + "` is incorrect." }}
    </strong>
  </div>
</template>

<script setup lang="ts">
import type { AnsweredQuestion } from "@/generated/backend"
import type { PropType } from "vue"

defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
})
</script>
