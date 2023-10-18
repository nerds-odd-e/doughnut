<template>
  <h2>Fine Tuning Questions Suggested by Users</h2>
  <button @click="downloadFineTuningJSONL()">
    Download Positive Feedback Question Generation Training Data
  </button>
  <button @click="downloadEvaluationJSONL()">
    Download Evaluation Training Data
  </button>
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

export default {
  setup() {
    return useLoadingApi();
  },
  data() {
    return {
      suggestedQuestions: undefined as
        | Generated.SuggestedQuestionForFineTuning[]
        | undefined,
    };
  },
  methods: {
    async downloadFineTuningJSONL() {
      const fineTuningData =
        await this.api.fineTuning.getPositiveFeedbackFineTuningExamples();
      const blob = new Blob(
        [fineTuningData.map((x) => JSON.stringify(x)).join("\n")],
        {
          type: "text/plain",
        },
      );
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "fineTuningData.jsonl";
      a.click();
      URL.revokeObjectURL(url);
    },
    async downloadEvaluationJSONL() {
      const fineTuningData =
        await this.api.fineTuning.getAllEvaluationModelExamples();

      const blob = new Blob(
        [fineTuningData.map((x) => JSON.stringify(x)).join("\n")],
        {
          type: "text/plain",
        },
      );
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "evaluationData.jsonl";
      a.click();
      URL.revokeObjectURL(url);
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
