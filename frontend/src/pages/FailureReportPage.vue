<template>
  <ContainerPage
    v-bind="{
      contentExists: !!failureReport,
      title: 'Failure Report',
    }"
  >
    <div v-if="!!failureReport">
      <div class="jumbotron py-4 mb-2">
        <h2><p v-text="failureReport.errorName" /></h2>
        <pre v-text="failureReport.errorDetail" />
        <p v-text="failureReport.createDatetime" />
        <a
          class="issue_link"
          :href="githubIssueUrl"
          title="show issue"
          v-text="githubIssueUrl"
        />
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
  props: { failureReportId: [String, Number] },
  components: { ContainerPage },
  data() {
    return {
      failureReport: null,
      githubIssueUrl: null,
    };
  },
  methods: {
    fetchData() {
      this.api.getFailureReport(this.failureReportId).then((res) => {
        this.failureReport = res.failureReport;
        this.githubIssueUrl = res.githubIssueUrl;
      });
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
