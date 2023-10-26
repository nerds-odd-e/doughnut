<template>
  <h2>Fine Tuning Questions Suggested by Users</h2>
  <button @click="downloadFineTuningJSONL()">
    Download Positive Feedback Question Generation Training Data
  </button>
  <button @click="downloadEvaluationJSONL()">
    Download Evaluation Training Data
  </button>
  <div width="100%">
    <textarea v-model="importingData" height="100px" />
  </div>
  <button @click="importData">Import</button>
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

const downloadJSONL = (
  fineTuningData: Generated.OpenAIChatGPTFineTuningExample[],
  filename: string,
) => {
  const blob = new Blob(
    [fineTuningData.map((x) => JSON.stringify(x)).join("\n")],
    {
      type: "text/plain",
    },
  );
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
};

export default {
  setup() {
    return useLoadingApi();
  },
  data() {
    return {
      suggestedQuestions: undefined as
        | Generated.SuggestedQuestionForFineTuning[]
        | undefined,
      importingData: "",
    };
  },
  methods: {
    importData() {
      const lines = this.importingData.split("\n");
      lines
        .map((x) => JSON.parse(x))
        .map(
          (x: Generated.OpenAIChatGPTFineTuningExample) =>
            <Generated.SuggestedQuestionForFineTuning>{
              preservedQuestion: JSON.parse(x.messages[2]!.content),
              comment: "imported",
              preservedNoteContent: x.messages[0]!.content,
            },
        )
        .forEach((x) => {
          this.api.fineTuning.createFeedback(x);
        });
    },
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
