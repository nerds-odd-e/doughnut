<template>
  <h1>Admin Dashboard</h1>
  <ul class="nav nav-tabs">
    <li class="nav-item">
      <a
        :class="`nav-link ${
          activePage === 'trainingQuestions' ? 'active' : ''
        }`"
        role="button"
        href="#"
        @click="activePage = 'trainingQuestions'"
        >Training Questions</a
      >
    </li>
    <li class="nav-item">
      <a
        :class="`nav-link ${activePage === 'failureReport' ? 'active' : ''}`"
        role="button"
        href="#"
        @click="activePage = 'failureReport'"
      >
        Failure Reports</a
      >
    </li>
  </ul>
  <div v-if="activePage === 'trainingQuestions'">
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
  </div>
  <FailureReportList v-if="activePage === 'failureReport'" />
</template>

<script lang="ts">
import { ContentLoader } from "vue-content-loader";
import useLoadingApi from "../managedApi/useLoadingApi";
import FailureReportList from "../components/admin/FailureReportList.vue";

export default {
  setup() {
    return useLoadingApi();
  },
  data() {
    return {
      activePage: undefined as
        | "trainingQuestions"
        | "failureReport"
        | undefined,
      suggestedQuestions: undefined as
        | Generated.SuggestedQuestionForFineTuning[]
        | undefined,
    };
  },
  watch: {
    async activePage() {
      if (this.activePage === "trainingQuestions") {
        this.suggestedQuestions =
          await this.api.getSuggestedQuestionsForFineTuning();
      }
    },
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
  components: { ContentLoader, FailureReportList },
};
</script>
