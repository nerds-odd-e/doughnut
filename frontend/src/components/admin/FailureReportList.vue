<template>
  <p v-if="!!errorMessage" v-text="errorMessage"></p>
  <ContainerPage v-else v-bind="{ contentLoaded: failureReports !== undefined }">
    <div v-if="!!failureReports">
      <h2>Failure report list</h2>
      <div v-if="selectedFailureReports.length > 0" class="mb-3">
        <button class="btn btn-danger" @click="deleteSelected">Delete Selected</button>
      </div>
      <div
        class="failure-report"
        v-for="element in failureReports"
        :key="element.id"
      >
        <div class="d-flex align-items-center">
          <input
            type="checkbox"
            :value="element.id"
            v-model="selectedFailureReports"
            class="me-2"
          />
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
      <div v-if="failureReports.length === 0" class="mt-3">
        No failure reports found.
      </div>
    </div>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue"
import type { FailureReport } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "@/pages/commons/ContainerPage.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  components: { ContainerPage },
  data() {
    return {
      failureReports: null as FailureReport[] | null,
      errorMessage: null as string | null,
      selectedFailureReports: [] as number[],
    }
  },
  methods: {
    fetchData() {
      this.managedApi.restFailureReportController
        .failureReports()
        .then((res) => {
          this.failureReports = res as unknown as FailureReport[]
        })
        .catch((err: { status?: number }) => {
          if (err.status === 401) {
            throw err
          }
          this.errorMessage = "It seems you cannot access this page."
        })
    },
    deleteSelected() {
      if (this.selectedFailureReports.length === 0) {
        return
      }

      this.managedApi.restFailureReportController
        .deleteFailureReports(this.selectedFailureReports)
        .then(() => {
          this.fetchData()
          this.selectedFailureReports = []
        })
        .catch((err: { status?: number }) => {
          if (err.status === 401) {
            throw err
          }
          this.errorMessage = "Error deleting failure reports."
        })
    },
  },
  mounted() {
    this.fetchData()
  },
})
</script>
