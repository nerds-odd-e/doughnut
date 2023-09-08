<!-- eslint-disable no-console -->
<!-- eslint-disable no-console -->
<template>
  <p v-if="!!errorMessage" v-text="errorMessage"></p>
  <div v-if="!!failureReports">
    <h2>Download questions marked as "good"</h2>
    <button class="download-button" @click="downloadTrainingData()">
      Download
    </button>
    <br />
    <h2>Download questions marked as "bad"</h2>
    <button class="download-button" @click="downloadBadTrainingData()">
      DownloadBad
    </button>
  </div>
</template>

<script>
import useLoadingApi from "../managedApi/useLoadingApi";
import ContainerPage from "./commons/ContainerPage.vue";
import ManagedApi from "../managedApi/ManagedApi";

export default {
  setup() {
    return useLoadingApi();
  },
  data() {
    return {
      failureReports: null,
      errorMessage: null,
    };
  },
  methods: {
    fetchData() {
      this.api
        .getFailureReports()
        .then((res) => {
          this.failureReports = res;
        })
        .catch(
          () => (this.errorMessage = "It seems you cannot access this page."),
        );
    },
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
    async downloadBadTrainingData() {
      try {
        // const response = await this.api.getTrainingData();
        const response = [];

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
        a.download = "badtrainingdata.txt";
        a.click();

        URL.revokeObjectURL(url);
      } catch (error) {
        return;
      }
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
