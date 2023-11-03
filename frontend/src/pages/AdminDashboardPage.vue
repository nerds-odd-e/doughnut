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
        Manage Model</a
      >
    </li>
    <li class="nav-item">
      <a
        :class="`nav-link ${
          activePage === 'evaluateQuestionModel' ? 'active' : ''
        }`"
        role="button"
        href="#"
        @click="activePage = 'evaluateQuestionModel'"
      >
        Evaluate Question Model</a
      >
    </li>
  </ul>
  <FineTuningData v-if="activePage === 'fineTuningData'" />
  <FailureReportList v-if="activePage === 'failureReport'" />
  <ManageModel v-if="activePage === 'manageModel'" />
  <EvaluateQuestionModel v-if="activePage === 'evaluateQuestionModel'" />
</template>

<script lang="ts">
import FineTuningData from "../components/admin/FineTuningData.vue";
import FailureReportList from "../components/admin/FailureReportList.vue";
import ManageModel from "../components/admin/ManageModel.vue";
import EvaluateQuestionModel from "../components/admin/EvaluateQuestionModel.vue";

export default {
  data() {
    return {
      activePage: "fineTuningData" as
        | "fineTuningData"
        | "failureReport"
        | "manageModel"
        | "evaluateQuestionModel"
        | undefined,
    };
  },
  components: {
    EvaluateQuestionModel,
    FineTuningData,
    FailureReportList,
    ManageModel,
  },
};
</script>
