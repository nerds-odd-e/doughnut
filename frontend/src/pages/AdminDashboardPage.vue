<template>
  <ContainerPage v-bind="{ title: 'Admin Dashboard' }" />
  <div class="daisy-tabs daisy-tabs-boxed daisy-bg-base-200 daisy-p-2 daisy-flex daisy-justify-center daisy-mb-4">
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'fineTuningData' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click="activePage = 'fineTuningData'"
    >Fine Tuning Data</a>
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'failureReport' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click="activePage = 'failureReport'"
    >Failure Reports</a>
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'manageModel' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click="activePage = 'manageModel'"
    >Manage Models</a>
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'manageBazaar' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click="activePage = 'manageBazaar'"
    >Manage Bazaar</a>
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'certificateRequests' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click="activePage = 'certificateRequests'"
    >Certification Requests</a>
  </div>
  <div class="daisy-container daisy-mx-auto">
    <FineTuningData v-if="activePage === 'fineTuningData'" />
    <FailureReportList v-if="activePage === 'failureReport'" />
    <ManageModel v-if="activePage === 'manageModel'" />
    <ManageBazaar v-if="activePage === 'manageBazaar'" />
    <CertificateRequests v-if="activePage === 'certificateRequests'" />
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import FineTuningData from "../components/admin/FineTuningData.vue"
import FailureReportList from "../components/admin/FailureReportList.vue"
import ManageModel from "../components/admin/ManageModel.vue"
import ManageBazaar from "../components/admin/ManageBazaar.vue"
import ContainerPage from "./commons/ContainerPage.vue"
import CertificateRequests from "../components/admin/CertificateRequests.vue"

const activePage = ref(
  "fineTuningData" as
    | "fineTuningData"
    | "failureReport"
    | "manageModel"
    | "manageBazaar"
    | "certificateRequests"
    | undefined
)
</script>
