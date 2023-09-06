<!-- eslint-disable no-console -->
<template>
  <p v-if="!!errorMessage" v-text="errorMessage"></p>
  <ContainerPage v-else v-bind="{ contentExists: !!failureReports }">
    <div v-if="!!failureReports">
      <h2>Failure report list</h2>
      <button @click="downloadTrainingData">Download</button>
      <div
        class="failure-report"
        v-for="element in failureReports"
        :key="element.id"
      >
        {{ element.createDatetime }} :
        <router-link
          :to="{
            name: 'failureReport',
            params: { failureReportId: element.id },
          }"
        >
          {{ element.errorName }}
        </router-link>
      </div>
    </div>
  </ContainerPage>
</template>

<script>
import useLoadingApi from "../managedApi/useLoadingApi";
import ContainerPage from "./commons/ContainerPage.vue";
import ManagedApi from "../managedApi/ManagedApi";

export default {
  setup() {
    return useLoadingApi();
  },
  components: { ContainerPage },
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
      // const apiUrl = "http://localhost:9081/api/gettrainingdata/goodtrainingdata";

      try {
        // const response = await fetch(apiUrl);
        const response = await this.api.getTrainingData();
        if (!response.ok) {
          throw new Error("Network response was not ok");
        }

        // to remove later
        // Create an object or array with dummy data
        const dummyData = [{ name: "1" }, { name: "2" }, { name: "3" }];

        // Convert the dummy data to a string
        const dummyDataString = JSON.stringify(dummyData);

        // Read the response text
        const data = await response.text();

        // Merge the response data and dummy data
        const finalData = `${data}\n${dummyDataString}`;

        // Create a Blob with the data
        const blob = new Blob([finalData], { type: "text/plain" });

        // Create a URL for the Blob
        const url = window.URL.createObjectURL(blob);

        // Create a link element and trigger the download
        const a = document.createElement("a");
        a.href = url;
        a.download = "trainingdata.txt"; // Specify the desired file name
        a.style.display = "none";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);

        // Clean up the URL object
        window.URL.revokeObjectURL(url);
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
