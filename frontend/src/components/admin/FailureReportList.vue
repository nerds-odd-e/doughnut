<template>
  <div v-if="!!errorMessage" class="daisy-alert daisy-alert-error">
    <svg
      xmlns="http://www.w3.org/2000/svg"
      class="daisy-stroke-current daisy-shrink-0 daisy-h-6 daisy-w-6"
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
    <div v-if="!!failureReports" class="daisy-space-y-4">
      <div class="daisy-flex daisy-items-center daisy-justify-between">
        <h2 class="daisy-text-2xl daisy-font-bold">
          Failure Reports
          <span
            v-if="failureReports.length > 0"
            class="daisy-badge daisy-badge-error daisy-ml-2"
          >
            {{ failureReports.length }}
          </span>
        </h2>
        <div class="daisy-flex daisy-gap-2">
          <button
            class="daisy-btn daisy-btn-warning daisy-btn-sm"
            @click="triggerFailure"
          >
            Trigger Test Exception
          </button>
          <button
            v-if="selectedFailureReports.length > 0"
            class="daisy-btn daisy-btn-error daisy-btn-sm"
            @click="showDeleteModal = true"
          >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="daisy-h-4 daisy-w-4"
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

      <div v-if="failureReports.length > 0" class="daisy-space-y-2">
        <div
          v-for="report in failureReports"
          :key="report.id"
          class="daisy-card daisy-bg-base-100 daisy-shadow-sm daisy-border daisy-border-base-300 hover:daisy-shadow-md daisy-transition-shadow"
        >
          <div
            class="daisy-card-body daisy-p-4 daisy-flex daisy-flex-row daisy-items-center daisy-gap-4"
          >
            <input
              type="checkbox"
              :value="report.id"
              v-model="selectedFailureReports"
              class="daisy-checkbox daisy-checkbox-error"
            />
            <div class="daisy-flex-1 daisy-min-w-0">
              <router-link
                :to="{
                  name: 'failureReport',
                  params: { failureReportId: report.id },
                }"
                class="daisy-link daisy-link-primary daisy-font-medium daisy-text-base hover:daisy-link-hover daisy-truncate daisy-block"
              >
                {{ report.errorName }}
              </router-link>
              <div
                class="daisy-text-sm daisy-text-base-content/60 daisy-mt-1 daisy-flex daisy-items-center daisy-gap-2"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="daisy-h-4 daisy-w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
                {{ formatDateTime(report.createDatetime) }}
              </div>
            </div>
            <div class="daisy-badge daisy-badge-ghost daisy-badge-sm">
              #{{ report.id }}
            </div>
          </div>
        </div>
      </div>

      <div
        v-else
        class="daisy-card daisy-bg-base-100 daisy-shadow-sm daisy-border daisy-border-base-300"
      >
        <div class="daisy-card-body daisy-items-center daisy-text-center">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="daisy-h-16 daisy-w-16 daisy-text-success daisy-mb-2"
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
          <h3 class="daisy-text-lg daisy-font-medium">All Clear!</h3>
          <p class="daisy-text-base-content/60">No failure reports found.</p>
        </div>
      </div>
    </div>

    <dialog
      class="daisy-modal"
      :class="{ 'daisy-modal-open': showDeleteModal }"
    >
      <div class="daisy-modal-box">
        <h3 class="daisy-font-bold daisy-text-lg">Confirm Deletion</h3>
        <p class="daisy-py-4">
          Are you sure you want to delete {{ selectedFailureReports.length }}
          failure report{{ selectedFailureReports.length > 1 ? "s" : "" }}? This
          action cannot be undone.
        </p>
        <div class="daisy-modal-action">
          <button class="daisy-btn" @click="showDeleteModal = false">
            Cancel
          </button>
          <button class="daisy-btn daisy-btn-error" @click="deleteSelected">
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
import { ref, onMounted } from "vue"
import type { FailureReport } from "@generated/backend"
import { FailureReportController } from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import ContainerPage from "@/pages/commons/ContainerPage.vue"

const failureReports = ref<FailureReport[] | null>(null)
const errorMessage = ref<string | null>(null)
const selectedFailureReports = ref<number[]>([])
const showDeleteModal = ref(false)

const formatDateTime = (dateTime: string) => {
  const date = new Date(dateTime)
  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  })
}

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
