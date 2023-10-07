<template>
  <h2>Fine Tuning Questions Suggested by Users</h2>
  <button @click="downloadFineTuningJSONL()">Download All Examples</button>
  <ContentLoader v-if="suggestedQuestions === undefined" />
  <div v-else>
    <table class="table">
      <thead>
        <tr>
          <th scope="col">Comment</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(suggested, index) in suggestedQuestions" :key="index">
          <td>{{ suggested.preservedQuestion.stem }}</td>
          <td>{{ suggested.comment }}</td>
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
    async downloadFineTuningJSONL() {
      const fineTuningData = await this.api.getFineTuningExamples();
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
  },
  components: { ContentLoader },
  async mounted() {
    this.suggestedQuestions =
      await this.api.getSuggestedQuestionsForFineTuning();
  },
};
</script>
