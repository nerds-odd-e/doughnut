<template>
  <ul class="nav nav-tabs">
    <li class="nav-item">
      <a
        :class="`nav-link ${
          activePage === 'trainingQuestions' ? 'active' : ''
        }`"
        aria-current="page"
        role="button"
        href="#"
        @click="activePage = 'trainingQuestions'"
        >Training Questions</a
      >
    </li>
    <li class="nav-item">
      <router-link :to="{ name: 'failureReportList' }"
        >Failure Reports</router-link
      >
    </li>
  </ul>
  <div v-if="activePage === 'trainingQuestions'">
    <h2>Fine Tuning Questions Suggested by Users</h2>
    <button class="download-button" @click="downloadTrainingData()">
      Download
    </button>
  </div>
</template>

<script lang="ts">
import useLoadingApi from "../managedApi/useLoadingApi";

export default {
  setup() {
    return useLoadingApi();
  },
  data() {
    return {
      activePage: undefined as string | undefined,
    };
  },
  methods: {
    async downloadTrainingData() {
      try {
        const response = await this.api.getTrainingData();

        // Create a Blob with the file content
        const blob = new Blob(
          [response.map((x) => JSON.stringify(x)).join("\n")],
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
      } catch (error) {
        return;
      }
    },
  },
};
</script>
