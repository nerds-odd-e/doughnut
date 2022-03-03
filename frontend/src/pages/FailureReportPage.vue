<template>
  <ContainerPage v-bind="{ loading, contentExists: !!failureReport, title: 'Failure Report' }">
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
import ContainerPage from "./commons/ContainerPage.vue";
import api from  "../managedApi/api";

export default {
  props: { failureReportId: [String, Number] },
  components: { ContainerPage },
  data() {
    return {
      loading: true,
      failureReport: null,
      githubIssueUrl: null,
    };
  },
  methods: {
    fetchData() {
      this.loading = true
      api().getFailureReport(this.failureReportId).then((res) => {
        this.failureReport = res.failureReport;
        this.githubIssueUrl = res.githubIssueUrl;
      })
      .finally(() => this.loading = false)
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
