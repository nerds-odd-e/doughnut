<template>
  <div v-if="answeredQuestion === null" class="daisy-alert daisy-alert-info">
    No answered question found for this memory tracker.
  </div>
  <AnsweredQuestionComponent
    v-else
    v-bind="{ answeredQuestion, conversationButton: true }"
  />
</template>

<script setup lang="ts">
import type { AnsweredQuestion } from "@generated/backend"
import AnsweredQuestionComponent from "@/components/review/AnsweredQuestionComponent.vue"

defineProps<{
  answeredQuestion: AnsweredQuestion | null
}>()
</script>

