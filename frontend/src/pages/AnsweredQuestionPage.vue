<template>
  <div class="daisy-container">
    <ContentLoader v-if="!answeredQuestion" />
    <AnsweredQuestionComponent
      v-else
      v-bind="{ answeredQuestion, conversationButton: true }"
    />
  </div>
</template>

<script setup lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import AnsweredQuestionComponent from "@/components/review/AnsweredQuestionComponent.vue"
import type { AnsweredQuestion } from "@generated/backend"
import { RecallPromptController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { onMounted, ref, watch, type PropType } from "vue"

const { recallPromptId } = defineProps({
  recallPromptId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
const answeredQuestion = ref<AnsweredQuestion | undefined>()

const fetchData = async () => {
  const { data: question, error } = await RecallPromptController.showQuestion({
    path: { recallPrompt: recallPromptId },
  })
  if (!error) {
    answeredQuestion.value = question!
  }
}

watch(() => recallPromptId, fetchData, { immediate: true })

onMounted(fetchData)
</script>
