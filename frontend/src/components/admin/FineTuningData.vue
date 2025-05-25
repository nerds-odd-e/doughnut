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
import type { SuggestedQuestionForFineTuning } from "@/generated/backend"
import { ApiError } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import SuggestedQuestionList from "./SuggestedQuestionList.vue"

export default {
  setup() {
    return useLoadingApi()
  },
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
      try {
        await this.managedApi.restFineTuningDataController.uploadAndTriggerFineTuning()
        this.fineTuningDataResultMsg = "Training initiated."
      } catch (error) {
        const errorInstance = error as ApiError
        this.fineTuningDataResultMsg = errorInstance.body.message
      }
      this.showAlert = true
    },
  },

  components: { ContentLoader, SuggestedQuestionList },
  async mounted() {
    this.suggestedQuestions =
      await this.managedApi.restFineTuningDataController.getAllSuggestedQuestions()
  },
}
</script>
