<template>
  <div
    role="alert"
    class="daisy-alert daisy-alert-success daisy-mt-6 daisy-shadow-lg"
    v-if="answeredQuestion.answer.correct"
  >
    <svg
      xmlns="http://www.w3.org/2000/svg"
      class="daisy-h-6 daisy-w-6 daisy-shrink-0 daisy-stroke-current"
      fill="none"
      viewBox="0 0 24 24"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2"
        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
      />
    </svg>
    <span class="daisy-font-semibold">Correct!</span>
  </div>
  <div
    role="alert"
    class="daisy-alert daisy-alert-danger daisy-mt-6 daisy-shadow-lg"
    v-else
  >
    <svg
      xmlns="http://www.w3.org/2000/svg"
      class="daisy-h-6 daisy-w-6 daisy-shrink-0 daisy-stroke-current"
      fill="none"
      viewBox="0 0 24 24"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2"
        d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"
      />
    </svg>
    <span class="daisy-font-semibold">
      {{ "Your answer `" + answeredQuestion.answerDisplay + "` is incorrect." }}
    </span>
  </div>
</template>

<script setup lang="ts">
import type { AnsweredQuestion } from "@generated/backend"
import type { PropType } from "vue"

defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
})
</script>
