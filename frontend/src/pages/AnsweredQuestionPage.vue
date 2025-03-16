<template>
  <div class="daisy:container">
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
