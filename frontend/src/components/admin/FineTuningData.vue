<template>
  <h2>Fine Tuning Questions Suggested by Users</h2>
  <button @click="downloadFineTuningJSONL()">
    Download Positive Feedback Question Generation Training Data
  </button>
  <button @click="downloadEvaluationJSONL()">
    Download Evaluation Training Data
  </button>
  <button>Upload Fine Tuning Training Data</button>
  <select>
    <option v-for="file in files" :key="file.id" :value="file.value">
      {{ file.text }}
    </option>
  </select>
  <button onclick="alert('Not implemented')">Trigger Fine Tuning</button>
  <ContentLoader v-if="suggestedQuestions === undefined" />
  <SuggestedQuestionList
    v-else
    :suggested-questions="suggestedQuestions"
    @duplicated="duplicated"
  />
</template>

<script lang="ts">
import { ContentLoader } from "vue-content-loader";
import useLoadingApi from "../../managedApi/useLoadingApi";
import SuggestedQuestionList from "./SuggestedQuestionList.vue";
import downloadJSONL from "./downloadJSONL";

export default {
  setup() {
    return useLoadingApi();
  },
  data() {
    return {
      suggestedQuestions: undefined as
        | Generated.SuggestedQuestionForFineTuning[]
        | undefined,
      files: [
        {
          id: 1,
          text: 1,
          value: 1,
        },
        {
          id: 1,
          text: 2,
          value: 3,
        },
      ],
    };
  },
  methods: {
    async downloadFineTuningJSONL() {
      const fineTuningData =
        await this.api.fineTuning.getPositiveFeedbackFineTuningExamples();
      downloadJSONL(fineTuningData, "fineTuningData.jsonl");
    },
    async downloadEvaluationJSONL() {
      const fineTuningData =
        await this.api.fineTuning.getAllEvaluationModelExamples();
      downloadJSONL(fineTuningData, "evaluationData.jsonl");
    },
    async duplicated(duplicated: Generated.SuggestedQuestionForFineTuning) {
      this.suggestedQuestions = [...this.suggestedQuestions!, duplicated];
    },
  },

  components: { ContentLoader, SuggestedQuestionList },
  async mounted() {
    this.suggestedQuestions =
      await this.api.fineTuning.getSuggestedQuestionsForFineTuning();
  },
};
</script>
