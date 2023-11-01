<template>
  <h2>Fine Tuning Questions Suggested by Users</h2>
  <button @click="downloadFineTuningJSONL()">
    Download Positive Feedback Question Generation Training Data
  </button>
  <button @click="downloadEvaluationJSONL()">
    Download Evaluation Training Data
  </button>
  <button @click="uploadFineTuningData">
    Upload Fine Tuning Training Data
  </button>
  <span v-if="showAlert">{{ fineTuningDataResultMsg }}</span>

  <select>
    <option
      v-for="aiTrainingFile in aiTrainingFiles"
      :key="aiTrainingFile.id"
      :value="aiTrainingFile.id"
    >
      {{ aiTrainingFile.filename }}
    </option>
  </select>
  <button @click="updateFineTuningResult()">Trigger Fine Tuning Success</button>
  <label title="fineTuningResult">{{ fineTuningResult }}</label>
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
      aiTrainingFiles: undefined as Generated.AiTrainingFile[] | undefined,
      fineTuningResult: undefined as string | undefined,
      fineTuningDataResultMsg: "",
      showAlert: false,
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
    updateFineTuningResult() {
      this.fineTuningResult = "successful";
    },
    async uploadFineTuningData() {
      const result = await this.api.fineTuning.postUploadFineTuningExamples();
      if (!result.success) {
        this.fineTuningDataResultMsg = result.message;
      } else {
        this.fineTuningDataResultMsg = "Upload successfully.";
      }
      this.showAlert = true;
    },
  },

  components: { ContentLoader, SuggestedQuestionList },
  async mounted() {
    this.fineTuningResult = "successful";
    this.aiTrainingFiles = await this.api.ai.getTrainingFiles();
    this.suggestedQuestions =
      await this.api.fineTuning.getSuggestedQuestionsForFineTuning();
  },
};
</script>
