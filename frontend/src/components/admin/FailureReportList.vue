<template>
  <div v-if="!!errorMessage" class="daisy-alert daisy-alert-error">
    <svg
      xmlns="http://www.w3.org/2000/svg"
      class="stroke-current shrink-0 h-6 w-6"
      fill="none"
      viewBox="0 0 24 24"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2"
        d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"
      />
    </svg>
    <span>{{ errorMessage }}</span>
  </div>
  <ContainerPage v-else v-bind="{ contentLoaded: failureReports !== undefined }">
    <div v-if="!!failureReports" class="space-y-4">
      <div class="flex items-center justify-between">
        <h2 class="text-2xl font-bold">
          Failure Reports
          <span
            v-if="failureReports.length > 0"
            class="daisy-badge daisy-badge-error ml-2"
          >
            {{ failureReports.length }}
          </span>
        </h2>
        <div class="flex gap-2">
          <button
            class="daisy-btn daisy-btn-warning daisy-btn-sm"
            @click="triggerFailure"
          >
            Trigger Test Exception
          </button>
          <button
            v-if="selectedFailureReports.length > 0"
            data-testid="failure-report-delete-selected"
            class="daisy-btn daisy-btn-error daisy-btn-sm"
            @click="showDeleteModal = true"
          >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-4 w-4"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
            />
          </svg>
          Delete Selected ({{ selectedFailureReports.length }})
          </button>
        </div>
      </div>

      <FailureReportListEntries
        v-if="failureReports.length > 0"
        :reports="failureReports"
        v-model:selected-ids="selectedFailureReports"
      />

      <div
        v-else
        class="daisy-card bg-base-100 shadow-sm border border-base-300"
      >
        <div class="daisy-card-body items-center text-center">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-16 w-16 text-success mb-2"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          <h3 class="text-lg font-medium">All Clear!</h3>
          <p class="text-base-content/60">No failure reports found.</p>
        </div>
      </div>
    </div>

    <dialog
      ref="deleteDialogRef"
      class="daisy-modal"
      :class="{ 'daisy-modal-open': showDeleteModal }"
      @close="showDeleteModal = false"
    >
      <div class="daisy-modal-box">
        <h3 class="font-bold text-lg">Confirm Deletion</h3>
        <p class="py-4">
          Are you sure you want to delete {{ selectedFailureReports.length }}
          failure report{{ selectedFailureReports.length > 1 ? "s" : "" }}? This
          action cannot be undone.
        </p>
        <div class="daisy-modal-action">
          <button
            data-testid="failure-report-delete-cancel"
            class="daisy-btn"
            @click="showDeleteModal = false"
          >
            Cancel
          </button>
          <button
            data-testid="failure-report-delete-confirm"
            class="daisy-btn daisy-btn-error"
            @click="deleteSelected"
          >
            Delete
          </button>
        </div>
      </div>
      <form method="dialog" class="daisy-modal-backdrop">
        <button @click="showDeleteModal = false">close</button>
      </form>
    </dialog>
  </ContainerPage>
</template>

<script setup lang="ts">
import { useDaisyDialog } from "@/composables/useDaisyDialog"
import { ref, onMounted } from "vue"
import type { FailureReport } from "@generated/donut-backend-api"
import { FailureReportController } from "@generated/donut-backend-api/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import FailureReportListEntries from "./FailureReportListEntries.vue"

const failureReports = ref<FailureReport[] | null>(null)
const errorMessage = ref<string | null>(null)
const selectedFailureReports = ref<number[]>([])
const showDeleteModal = ref(false)
const deleteDialogRef = ref<HTMLDialogElement | null>(null)
useDaisyDialog(showDeleteModal, deleteDialogRef)

const fetchData = async () => {
  const { data: reports, error } = await apiCallWithLoading(() =>
    FailureReportController.failureReports({})
  )
  if (!error) {
    const sortedReports = (reports as unknown as FailureReport[]).sort(
      (a, b) => {
        const dateA = new Date(a.createDatetime).getTime()
        const dateB = new Date(b.createDatetime).getTime()
        return dateB - dateA
      }
    )
    failureReports.value = sortedReports
  } else {
    const errorObj = toOpenApiError(error)
    const errMessage =
      errorObj.message || "It seems you cannot access this page."
    const errorWithStatus = error as unknown as { status?: number }
    if (errorWithStatus?.status === 401) {
      throw error
    }
    errorMessage.value = errMessage
  }
}

const triggerFailure = async () => {
  await apiCallWithLoading(() => FailureReportController.triggerFailure({}))
  await fetchData()
}

const deleteSelected = async () => {
  if (selectedFailureReports.value.length === 0) {
    return
  }

  const { error } = await apiCallWithLoading(() =>
    FailureReportController.deleteFailureReports({
      body: selectedFailureReports.value,
    })
  )
  if (!error) {
    showDeleteModal.value = false
    await fetchData()
    selectedFailureReports.value = []
  }
}

onMounted(() => {
  fetchData()
})
</script>
