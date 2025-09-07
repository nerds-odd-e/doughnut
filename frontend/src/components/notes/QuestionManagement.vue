<template>
  <PopButton btn-class="btn-primary">
    <template #button_face>
      Send this question for fine tuning the question generation model
    </template>
    <template #default="{ closer }">
      <SuggestQuestionForFineTuning
        :predefined-question="predefinedQuestion"
        @close-dialog="closer()"
      />
    </template>
  </PopButton>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { PredefinedQuestion } from "@generated/backend"
import SuggestQuestionForFineTuning from "../ai/SuggestQuestionForFineTuning.vue"

defineProps({
  predefinedQuestion: {
    type: Object as PropType<PredefinedQuestion>,
    required: true,
  },
})
</script>
