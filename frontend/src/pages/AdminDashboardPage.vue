<template>
  <h1>Admin Dashboard</h1>
  <ul class="nav nav-tabs">
    <li class="nav-item">
      <a
        :class="`nav-link ${activePage === 'fineTuningData' ? 'active' : ''}`"
        role="button"
        href="#"
        @click="activePage = 'fineTuningData'"
        >Fine Tuning Data</a
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
    <li class="nav-item">
      <a
        :class="`nav-link ${activePage === 'manageModel' ? 'active' : ''}`"
        role="button"
        href="#"
        @click="activePage = 'manageModel'"
      >
        Manage Models</a
      >
    </li>
    <li class="nav-item">
      <a
        :class="`nav-link ${activePage === 'manageAssistant' ? 'active' : ''}`"
        role="button"
        href="#"
        @click="activePage = 'manageAssistant'"
      >
        Manage Assistant</a
      >
    </li>
  </ul>
  <FineTuningData v-if="activePage === 'fineTuningData'" />
  <FailureReportList v-if="activePage === 'failureReport'" />
  <ManageModel v-if="activePage === 'manageModel'" />
  <ManageAssistant v-if="activePage === 'manageAssistant'" />
</template>

<script lang="ts">
import FineTuningData from "../components/admin/FineTuningData.vue";
import FailureReportList from "../components/admin/FailureReportList.vue";
import ManageModel from "../components/admin/ManageModel.vue";
import ManageAssistant from "../components/admin/ManageAssistant.vue";

export default {
  data() {
    return {
      activePage: "fineTuningData" as
        | "fineTuningData"
        | "failureReport"
        | "manageModel"
        | "manageAssistant"
        | undefined,
    };
  },
  components: {
    FineTuningData,
    FailureReportList,
    ManageModel,
    ManageAssistant,
  },
};
</script>
