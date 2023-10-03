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
  <FineTuningData v-if="activePage === 'trainingQuestions'" />
  <FailureReportList v-if="activePage === 'failureReport'" />
</template>

<script lang="ts">
import FineTuningData from "../components/admin/FineTuningData.vue";
import FailureReportList from "../components/admin/FailureReportList.vue";

export default {
  data() {
    return {
      activePage: undefined as
        | "trainingQuestions"
        | "failureReport"
        | undefined,
    };
  },
  components: { FineTuningData, FailureReportList },
};
</script>
