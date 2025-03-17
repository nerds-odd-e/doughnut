<template>
  <ContainerPage v-bind="{ title: 'Admin Dashboard' }" />
  <div class="daisy:tabs">
    <a
      :class="`daisy:tab ${activePage === 'fineTuningData' ? 'daisy:tab-active' : ''}`"
      role="button"
      href="#"
      @click="activePage = 'fineTuningData'"
    >Fine Tuning Data</a>
    <a
      :class="`daisy:tab ${activePage === 'failureReport' ? 'daisy:tab-active' : ''}`"
      role="button"
      href="#"
      @click="activePage = 'failureReport'"
    >Failure Reports</a>
    <a
      :class="`daisy:tab ${activePage === 'manageModel' ? 'daisy:tab-active' : ''}`"
      role="button"
      href="#"
      @click="activePage = 'manageModel'"
    >Manage Models</a>
    <a
      :class="`daisy:tab ${activePage === 'manageAssistant' ? 'daisy:tab-active' : ''}`"
      role="button"
      href="#"
      @click="activePage = 'manageAssistant'"
    >Manage Assistant</a>
    <a
      :class="`daisy:tab ${activePage === 'manageBazaar' ? 'daisy:tab-active' : ''}`"
      role="button"
      href="#"
      @click="activePage = 'manageBazaar'"
    >Manage Bazaar</a>
    <a
      :class="`daisy:tab ${activePage === 'certificateRequests' ? 'daisy:tab-active' : ''}`"
      role="button"
      href="#"
      @click="activePage = 'certificateRequests'"
    >Certification Requests</a>
  </div>
  <FineTuningData v-if="activePage === 'fineTuningData'" />
  <FailureReportList v-if="activePage === 'failureReport'" />
  <ManageModel v-if="activePage === 'manageModel'" />
  <ManageAssistant v-if="activePage === 'manageAssistant'" />
  <ManageBazaar v-if="activePage === 'manageBazaar'" />
  <CertificateRequests v-if="activePage === 'certificateRequests'" />
</template>

<script setup lang="ts">
import { ref } from "vue"
import FineTuningData from "../components/admin/FineTuningData.vue"
import FailureReportList from "../components/admin/FailureReportList.vue"
import ManageModel from "../components/admin/ManageModel.vue"
import ManageAssistant from "../components/admin/ManageAssistant.vue"
import ManageBazaar from "../components/admin/ManageBazaar.vue"
import ContainerPage from "./commons/ContainerPage.vue"
import CertificateRequests from "../components/admin/CertificateRequests.vue"

const activePage = ref(
  "fineTuningData" as
    | "fineTuningData"
    | "failureReport"
    | "manageModel"
    | "manageAssistant"
    | "manageBazaar"
    | "certificateRequests"
    | undefined
)
</script>
