<template>
  <h2>Fine Tuning Questions Suggested by Users</h2>

  <button @click="triggerFineTuning()">Trigger Fine Tuning</button>
  <span v-if="showAlert">{{ fineTuningDataResultMsg }}</span>
  <ContentLoader v-if="suggestedQuestions === undefined" />
  <SuggestedQuestionList
    v-else
    :suggested-questions="suggestedQuestions"
    @duplicated="duplicated"
  />
</template>

<script lang="ts">
import { ContentLoader } from "vue-content-loader"
import type { SuggestedQuestionForFineTuning } from "@generated/backend"
import { FineTuningDataController } from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import SuggestedQuestionList from "./SuggestedQuestionList.vue"

export default {
  data() {
    return {
      suggestedQuestions: undefined as
        | SuggestedQuestionForFineTuning[]
        | undefined,
      fineTuningDataResultMsg: "",
      showAlert: false,
      fileId: "",
    }
  },
  methods: {
    async duplicated(duplicated: SuggestedQuestionForFineTuning) {
      this.suggestedQuestions = [...this.suggestedQuestions!, duplicated]
    },
    async triggerFineTuning() {
      const { error } =
        await FineTuningDataController.uploadAndTriggerFineTuning()
      if (!error) {
        this.fineTuningDataResultMsg = "Training initiated."
      } else {
        // Error is handled by global interceptor (toast notification)
        // Extract error message for display
        const errorObj = toOpenApiError(error)
        this.fineTuningDataResultMsg =
          errorObj.message || "Failed to trigger fine tuning"
      }
      this.showAlert = true
    },
  },

  components: { ContentLoader, SuggestedQuestionList },
  async mounted() {
    const { data: questions, error } =
      await FineTuningDataController.getAllSuggestedQuestions()
    if (!error && questions) {
      this.suggestedQuestions = questions
    }
  },
}
</script>
