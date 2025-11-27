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
import { FailureReportController } from "@generated/backend/sdk.gen"
import { globalClientSilent } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"
import ContainerPage from "@/pages/commons/ContainerPage.vue"

export default defineComponent({
  components: { ContainerPage },
  data() {
    return {
      failureReports: null as FailureReport[] | null,
      errorMessage: null as string | null,
      selectedFailureReports: [] as number[],
    }
  },
  methods: {
    async fetchData() {
      const { data: reports, error } =
        await FailureReportController.failureReports({
          client: globalClientSilent,
        })
      if (!error) {
        this.failureReports = reports as unknown as FailureReport[]
      } else {
        // Error is handled by global interceptor (toast notification)
        // Extract error message for display
        const errorObj = toOpenApiError(error)
        const errorMessage =
          errorObj.message || "It seems you cannot access this page."
        // Check if it's a 401 error (handled by global interceptor)
        const errorWithStatus = error as unknown as { status?: number }
        if (errorWithStatus?.status === 401) {
          throw error
        }
        this.errorMessage = errorMessage
      }
    },
    async deleteSelected() {
      if (this.selectedFailureReports.length === 0) {
        return
      }

      const { error } = await FailureReportController.deleteFailureReports({
        body: this.selectedFailureReports,
      })
      if (!error) {
        this.fetchData()
        this.selectedFailureReports = []
      }
    },
  },
  mounted() {
    this.fetchData()
  },
})
</script>
