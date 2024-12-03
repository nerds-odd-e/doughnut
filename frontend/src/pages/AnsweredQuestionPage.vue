<template>
  <div class="container">
    <ContentLoader v-if="!answeredQuestion" />
    <AnsweredQuestionComponent
      v-else
      v-bind="{ answeredQuestion }"
    />
  </div>
</template>

<script setup lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import AnsweredQuestionComponent from "@/components/review/AnsweredQuestionComponent.vue"
import type { AnsweredQuestion } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { onMounted, ref, watch, type PropType } from "vue"

const { managedApi } = useLoadingApi()

const { recallPromptId } = defineProps({
  recallPromptId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
const answeredQuestion = ref<AnsweredQuestion | undefined>()

const fetchData = async () => {
  answeredQuestion.value =
    await managedApi.restRecallPromptController.showQuestion(recallPromptId)
}

watch(() => recallPromptId, fetchData, { immediate: true })

onMounted(fetchData)
</script>

<style scoped>
.conversation-button {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #4CAF50;
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 5px rgba(0,0,0,0.2);
  transition: background-color 0.3s;
}

.conversation-button:hover {
  background-color: #45a049;
}
</style>
