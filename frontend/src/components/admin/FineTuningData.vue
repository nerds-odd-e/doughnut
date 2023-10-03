<template>
  <h2>Fine Tuning Questions Suggested by Users</h2>
  <button class="download-button" @click="downloadTrainingData()">
    Download
  </button>
  <ContentLoader v-if="suggestedQuestions === undefined" />
  <div v-else>
    <table class="table">
      <thead>
        <tr>
          <th scope="col">Comment</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(question, index) in suggestedQuestions" :key="index">
          <td>{{ question.comment }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script lang="ts">
import { ContentLoader } from "vue-content-loader";
import useLoadingApi from "../../managedApi/useLoadingApi";

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
    async downloadTrainingData() {
      const trainingData = await this.api.getTrainingData();
      const blob = new Blob(
        [trainingData.map((x) => JSON.stringify(x)).join("\n")],
        {
          type: "text/plain",
        },
      );
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "trainingdata.txt";
      a.click();
      URL.revokeObjectURL(url);
    },
  },
  components: { ContentLoader },
  async mounted() {
    this.suggestedQuestions =
      await this.api.getSuggestedQuestionsForFineTuning();
  },
};
</script>
