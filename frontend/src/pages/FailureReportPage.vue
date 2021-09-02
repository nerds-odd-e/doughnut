<template>
  <h2>Failure Report</h2>

  <LoadingPage v-bind="{ loading, contentExists: !!failureReport }">
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
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import { restGet } from "../restful/restful";

export default {
  props: { failureReportId: String },
  components: { LoadingPage },
  data() {
    return {
      loading: false,
      failureReport: null,
      githubIssueUrl: null,
    };
  },
  methods: {
    fetchData() {
      restGet(
        `/api/failure-reports/${this.failureReportId}`,
        (r) => (this.loading = r)
      ).then((res) => {
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
