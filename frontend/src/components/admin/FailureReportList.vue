<template>
  <p v-if="!!errorMessage" v-text="errorMessage"></p>
  <ContainerPage
    v-else
    v-bind="{
      contentExists: !!failureReports,
      title: 'Failure Report List',
    }"
  >
    <div v-if="!!failureReports">
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
import useLoadingApi from "@/managedApi/useLoadingApi";
import ContainerPage from "@/pages/commons/ContainerPage.vue";

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
      this.managedApi.restFailureReportController
        .failureReports()
        .then((res) => {
          this.failureReports = res;
        })
        .catch((err) => {
          if (err.status === 401) {
            throw err;
          }
          this.errorMessage = "It seems you cannot access this page.";
        });
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
