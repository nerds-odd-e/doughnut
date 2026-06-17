<template>
  <ContainerPage v-bind="{ title: 'Admin Dashboard' }" />
  <div class="daisy-tabs daisy-tabs-box bg-base-200 p-2 flex justify-center mb-4">
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'failureReport' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click.prevent="setActivePage('failureReport')"
    >Failure Reports</a>
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'manageModel' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click.prevent="setActivePage('manageModel')"
    >Manage Models</a>
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'manageBazaar' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click.prevent="setActivePage('manageBazaar')"
    >Manage Bazaar</a>
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'users' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click.prevent="setActivePage('users')"
    >Users</a>
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'dataMigration' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click.prevent="setActivePage('dataMigration')"
    >Data migration</a>
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'batchQuestions' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click.prevent="setActivePage('batchQuestions')"
    >Batch Questions</a>
  </div>
  <div class="container mx-auto">
    <FailureReportList v-if="activePage === 'failureReport'" />
    <ManageModel v-if="activePage === 'manageModel'" />
    <ManageBazaar v-if="activePage === 'manageBazaar'" />
    <UserListing v-if="activePage === 'users'" />
    <DataMigrationPanel v-if="activePage === 'dataMigration'" />
    <QuestionGenerationBatchStatus v-if="activePage === 'batchQuestions'" />
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { useRoute, useRouter } from "vue-router"
import FailureReportList from "../components/admin/FailureReportList.vue"
import ManageModel from "../components/admin/ManageModel.vue"
import ManageBazaar from "../components/admin/ManageBazaar.vue"
import ContainerPage from "./commons/ContainerPage.vue"
import UserListing from "../components/admin/UserListing.vue"
import DataMigrationPanel from "../components/admin/DataMigrationPanel.vue"
import QuestionGenerationBatchStatus from "../components/admin/QuestionGenerationBatchStatus.vue"

type TabType =
  | "failureReport"
  | "manageModel"
  | "manageBazaar"
  | "users"
  | "dataMigration"
  | "batchQuestions"

const route = useRoute()
const router = useRouter()

const activePage = computed({
  get(): TabType {
    const tab = route.query.tab as string | undefined
    if (
      tab === "failureReport" ||
      tab === "manageModel" ||
      tab === "manageBazaar" ||
      tab === "users" ||
      tab === "dataMigration" ||
      tab === "batchQuestions"
    ) {
      return tab
    }
    return "failureReport"
  },
  set(value: TabType) {
    router.push({
      name: "adminDashboard",
      query: { tab: value },
    })
  },
})

const setActivePage = (tab: TabType) => {
  activePage.value = tab
}
</script>
