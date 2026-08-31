<template>
  <div class="flex items-center gap-2 px-1">
    <label class="daisy-label cursor-pointer flex items-center gap-2 m-0 p-0">
      <input
        ref="selectAllCheckboxRef"
        data-testid="failure-report-select-all"
        type="checkbox"
        class="daisy-checkbox daisy-checkbox-error"
        :checked="allFailureReportsSelected"
        @change="onSelectAllChange"
      />
      <span class="daisy-label-text">Select all</span>
    </label>
  </div>

  <div class="space-y-2">
    <div
      v-for="report in reports"
      :key="report.id"
      class="daisy-card bg-base-100 shadow-sm border border-base-300 hover:shadow-md transition-shadow"
    >
      <div class="daisy-card-body p-4 flex flex-row items-center gap-4">
        <input
          data-testid="failure-report-row-select"
          type="checkbox"
          :value="report.id"
          v-model="selectedIds"
          class="daisy-checkbox daisy-checkbox-error"
        />
        <div class="flex-1 min-w-0">
          <router-link
            :to="{
              name: 'failureReport',
              params: { failureReportId: report.id },
            }"
            class="daisy-link daisy-link-primary font-medium text-base hover:daisy-link-hover truncate block"
          >
            {{ report.errorName }}
          </router-link>
          <div
            class="text-sm text-base-content/60 mt-1 flex items-center gap-2"
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
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
            {{ formatDateTime(report.createDatetime) }}
            <FailureReportOccurrenceCount :count="report.occurrenceCount" />
          </div>
        </div>
        <div class="daisy-badge daisy-badge-ghost daisy-badge-sm">
          #{{ report.id }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue"
import type { FailureReport } from "@generated/donut-backend-api"
import FailureReportOccurrenceCount from "./FailureReportOccurrenceCount.vue"

const props = defineProps<{
  reports: FailureReport[]
}>()

const selectedIds = defineModel<number[]>("selectedIds", { required: true })
const selectAllCheckboxRef = ref<HTMLInputElement | null>(null)

const allFailureReportsSelected = computed(() => {
  if (!props.reports.length) {
    return false
  }
  return selectedIds.value.length === props.reports.length
})

const syncSelectAllIndeterminate = () => {
  const el = selectAllCheckboxRef.value
  if (!el) {
    return
  }
  if (!props.reports.length) {
    el.indeterminate = false
    return
  }
  const n = selectedIds.value.length
  el.indeterminate = n > 0 && n < props.reports.length
}

watch(
  [() => props.reports, selectedIds],
  () => {
    nextTick(() => {
      syncSelectAllIndeterminate()
    })
  },
  { deep: true }
)

const onSelectAllChange = (event: Event) => {
  const checked = (event.target as HTMLInputElement).checked
  if (!props.reports.length) {
    return
  }
  if (checked) {
    selectedIds.value = props.reports
      .map((r) => r.id)
      .filter((id): id is number => id !== undefined)
  } else {
    selectedIds.value = []
  }
}

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
</script>
