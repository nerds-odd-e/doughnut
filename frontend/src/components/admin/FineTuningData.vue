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

  <select id="list" v-model="fileId">
    <option
      v-for="aiTrainingFile in aiTrainingFiles"
      :key="aiTrainingFile.id"
      :value="aiTrainingFile.id"
    >
      {{ aiTrainingFile.filename }}
    </option>
  </select>
  <button @click="getTrainingFiles()">Retrieve</button>
  <button @click="triggerFineTuning()">Trigger Fine Tuning</button>
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
      fileId: "",
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
    async triggerFineTuning() {
      const apiResponse = await this.api.ai.triggerFineTuning(this.fileId);
      this.fineTuningResult = apiResponse.message;
    },
    async uploadFineTuningData() {
      try {
        await this.api.fineTuning.postUploadFineTuningExamples();
        this.fineTuningDataResultMsg = "Upload successfully.";
      } catch (error) {
        const errorInstance = error as Error;
        this.fineTuningDataResultMsg = errorInstance.message;
      }
      this.showAlert = true;
    },
    async getTrainingFiles() {
      this.aiTrainingFiles = await this.api.ai.getTrainingFiles();
      this.fileId = this.aiTrainingFiles[0] ? this.aiTrainingFiles[0].id : "";
    },
  },

  components: { ContentLoader, SuggestedQuestionList },
  async mounted() {
    this.suggestedQuestions =
      await this.api.fineTuning.getSuggestedQuestionsForFineTuning();
  },
};
</script>
