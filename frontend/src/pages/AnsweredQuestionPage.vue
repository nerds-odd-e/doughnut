<template>
  <div class="container">
    <ContentLoader v-if="!answeredQuestion" />
    <AnsweredQuestionComponent
      v-else
      v-bind="{ answeredQuestion, storageAccessor }"
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

const { reviewQuestionInstanceId } = defineProps({
  reviewQuestionInstanceId: { type: Number, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
const answeredQuestion = ref<AnsweredQuestion | undefined>()

const fetchData = async () => {
  answeredQuestion.value =
    await managedApi.restReviewQuestionController.showQuestion(
      reviewQuestionInstanceId
    )
}

watch(() => reviewQuestionInstanceId, fetchData, { immediate: true })

onMounted(fetchData)
</script>
