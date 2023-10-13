<template>
  <h2>Fine Tuning Questions Suggested by Users</h2>
  <button @click="downloadFineTuningJSONL()">
    Download Positive Feedback Question Generation Training Data
  </button>
  <button @click="downloadEvaluationJSONL()">
    Download Evaluation Training Data
  </button>
  <ContentLoader v-if="suggestedQuestions === undefined" />
  <div v-else>
    <table class="table">
      <thead>
        <tr>
          <th scope="col">Stem</th>
          <th scope="col">Feedback</th>
          <th scope="col">Comment</th>
          <th scope="col">Operation</th>
          <th scope="col">Is Duplicated</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(suggested, index) in suggestedQuestions"
          :key="index"
          @dblclick="editSuggestedQuestion(index)"
        >
          <td>{{ suggested.preservedQuestion.stem }}</td>
          <td>{{ suggested.positiveFeedback ? "Positive" : "Negative" }}</td>
          <td>{{ suggested.comment }}</td>
          <td>
            <button
              v-if="suggested.positiveFeedback == false"
              :id="`duplicate-${index}`"
              class="btn btn-primary"
              @click="duplicateQuestion(suggested)"
            >
              Duplicate
            </button>
          </td>
          <td :id="`is-duplicated-${index}`">
            {{ suggested.duplicated ? "Yes" : "No" }}
          </td>
        </tr>
      </tbody>
    </table>
    <Popup v-model="showEditDialog">
      <SuggestedQuestionEdit
        v-if="currentIndex !== undefined"
        v-model="suggestedQuestions[currentIndex]"
        :key="currentIndex"
      />
    </Popup>
  </div>
</template>

<script lang="ts">
import { ContentLoader } from "vue-content-loader";
import useLoadingApi from "../../managedApi/useLoadingApi";
import Popup from "../commons/Popups/Popup.vue";

export default {
  setup() {
    return useLoadingApi();
  },
  data() {
    return {
      suggestedQuestions: undefined as
        | Generated.SuggestedQuestionForFineTuning[]
        | undefined,
      currentIndex: undefined as number | undefined,
      showEditDialog: false,
    };
  },
  methods: {
    async editSuggestedQuestion(index: number) {
      this.currentIndex = index;
      this.showEditDialog = true;
    },
    async downloadFineTuningJSONL() {
      const fineTuningData =
        await this.api.getPositiveFeedbackFineTuningExamples();
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
      const fineTuningData = await this.api.getAllEvaluationModelExamples();

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
    async duplicateQuestion(
      suggested: Generated.SuggestedQuestionForFineTuning,
    ) {
      await this.api.reviewMethods.suggestQuestionForFineTuning(
        suggested.quizQuestionId ?? -1,
        {
          isPositiveFeedback: true,
          comment: suggested.comment,
          isDuplicated: true,
        },
      );

      this.suggestedQuestions =
        await this.api.getSuggestedQuestionsForFineTuning();
    },
  },

  components: { ContentLoader, Popup },
  async mounted() {
    this.suggestedQuestions =
      await this.api.getSuggestedQuestionsForFineTuning();
  },
};
</script>
