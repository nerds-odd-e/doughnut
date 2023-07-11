<template>
  <p v-if="!!errorMessage" v-text="errorMessage"></p>
  <ContainerPage v-else v-bind="{ contentExists: !!failureReports }">
    <div v-if="!!failureReports">
      <h2>Failure report list</h2>
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
  },
  mounted() {
    this.fetchData();
  },
};
</script>
