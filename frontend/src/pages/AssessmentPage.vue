<template>
  <ContainerPage v-bind="{ contentLoaded: loaded, title: `Assessment For ${ assessmentAttempt?.notebookTitle }`}">
    <Assessment v-if="assessmentAttempt" :assessment-attempt="assessmentAttempt" />
  </ContainerPage>
  <div v-if="errors" class="daisy-alert daisy-alert-danger">{{ errors }}</div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import ContainerPage from "./commons/ContainerPage.vue"
import Assessment from "@/components/assessment/Assessment.vue"
import { AssessmentController } from "@generated/backend/sdk.gen"
import type { AssessmentAttempt } from "@generated/backend"
import { toOpenApiError } from "@/managedApi/openApiError"

const props = defineProps({
  notebookId: { type: Number, required: true },
})

const loaded = ref(false)
const assessmentAttempt = ref<AssessmentAttempt | undefined>()
const errors = ref("")

const generateAssessmentQuestions = async () => {
  const { data: attempt, error } =
    await AssessmentController.generateAssessmentQuestions({
      path: { notebook: props.notebookId },
    })
  if (!error) {
    assessmentAttempt.value = attempt!
  } else {
    // Error is handled by global interceptor (toast notification)
    // Extract error message for display
    const errorObj = toOpenApiError(error)
    errors.value = errorObj.message || "Failed to generate assessment questions"
  }
  loaded.value = true
}

onMounted(() => {
  generateAssessmentQuestions()
})
</script>
