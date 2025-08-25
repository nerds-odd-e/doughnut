<template>
  <div>
    <div v-for="(question, index) in questions" :key="index" class="flex items-center space-x-2 mb-2">
      <input
        type="checkbox"
        :id="`question-${index}`"
        :value="question"
        v-model="questionsToDelete"
        class="checkbox checkbox-primary"
      />
      <label :for="`question-${index}`" class="text-base">{{ question.multipleChoicesQuestion.stem }}</label>
    </div>

    <button
      @click="submitDelete"
      class="daisy-btn daisy-btn-sm daisy-btn-primary"
    >
      Submit
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from "vue";
import type { PredefinedQuestion } from "@/generated/backend";

const props = defineProps({
  questions: {
    type: Array as () => PredefinedQuestion[],
    required: true,
  },
});

const questionsToDelete = ref<PredefinedQuestion[]>([]);

watchEffect(() => {
  questionsToDelete.value = [...props.questions];
});

const emit = defineEmits(["close-dialog"]);

const submitDelete = () => {
  emit("close-dialog", questionsToDelete.value);
  questionsToDelete.value = []
};
</script>