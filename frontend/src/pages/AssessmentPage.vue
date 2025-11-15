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
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { AssessmentAttempt } from "@generated/backend"

const { managedApi } = useLoadingApi()
const props = defineProps({
  notebookId: { type: Number, required: true },
})

const loaded = ref(false)
const assessmentAttempt = ref<AssessmentAttempt | undefined>()
const errors = ref("")

const generateAssessmentQuestions = async () => {
  try {
    assessmentAttempt.value =
      await managedApi.services.generateAssessmentQuestions({
        notebook: props.notebookId,
      })
  } catch (err) {
    if (err instanceof Error) {
      errors.value = err.message
    } else {
      errors.value = String(err)
    }
  }
  loaded.value = true
}

onMounted(() => {
  generateAssessmentQuestions()
})
</script>
