<template>
  <h2>Download questions marked by users</h2>
  <button class="download-button" @click="downloadTrainingData()">
    Download
  </button>
  <p>
    <router-link :to="{ name: 'failureReportList' }"
      >Failure Reports</router-link
    >
  </p>
</template>

<script>
import useLoadingApi from "../managedApi/useLoadingApi";
import ManagedApi from "../managedApi/ManagedApi";

export default {
  setup() {
    return useLoadingApi();
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
