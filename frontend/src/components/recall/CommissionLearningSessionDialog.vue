<template>
  <Modal @close_request="$emit('close')">
    <template #body>
      <div data-test="commission-learning-session-dialog" class="daisy-card">
        <div class="daisy-card-body">
          <h3 class="daisy-card-title text-xl font-semibold">
            Learning session request
          </h3>
          <p class="text-sm text-base-content mt-2">
            Notebook: "{{ notebookName }}"
          </p>
          <div
            v-if="loading"
            class="flex justify-center items-center h-96 mt-4"
            data-test="learning-session-request-loading"
          >
            <span
              class="daisy-loading daisy-loading-spinner daisy-loading-lg"
            />
          </div>
          <template v-else-if="requestReady">
            <p class="text-sm mt-4">Learning session request</p>
            <textarea
              class="daisy-textarea w-full h-96 bg-base-100 font-mono text-xs mt-2"
              readonly
              :value="requestMarkdown"
              data-test="learning-session-request"
            />
            <div class="flex gap-2 justify-end mt-2">
              <CopyButton
                :text="requestMarkdown"
                :disabled="!requestMarkdown"
                test-id="copy-learning-session-request"
                aria-label="Copy learning session request"
              />
            </div>
            <div
              v-if="status === 'AWAITING_REPORT'"
              class="daisy-alert daisy-alert-info mt-4"
              data-test="learning-session-awaiting-report"
            >
              <span>This learning session is awaiting the tutor's report.</span>
            </div>
            <div
              v-if="status === 'RECORDED'"
              class="daisy-alert daisy-alert-info mt-4"
              data-test="learning-session-recorded"
            >
              <span>This learning session is recorded.</span>
            </div>
            <div
              v-if="rejectedEntries.length > 0"
              class="daisy-alert daisy-alert-warning mt-4"
              data-test="learning-session-report-rejections"
            >
              <div class="flex flex-col gap-1">
                <span
                  v-for="(entry, index) in rejectedEntries"
                  :key="index"
                >
                  {{ entry.line }} — {{ entry.reason }}
                </span>
              </div>
            </div>
            <p class="text-sm mt-4">Learning session report</p>
            <textarea
              v-model="reportMarkdown"
              class="daisy-textarea w-full h-48 bg-base-100 font-mono text-xs mt-2"
              data-test="learning-session-report"
            />
            <button
              type="button"
              class="daisy-btn daisy-btn-primary mt-4"
              data-test="record-learning-session-report-submit"
              @click="recordReport"
            >
              Record report
            </button>
          </template>
        </div>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue"
import { LearningSessionController } from "@generated/doughnut-backend-api/sdk.gen"
import type {
  LearningSessionCommissionResponse,
  RejectedLearningSessionReportEntry,
} from "@generated/doughnut-backend-api/types.gen"
import Modal from "@/components/commons/Modal.vue"
import CopyButton from "@/components/commons/CopyButton.vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import timezoneParam from "@/managedApi/window/timezoneParam"

const props = defineProps<{
  notebookId: number
  notebookName: string
  mode?: "request" | "record" | "amend"
  initialRequestMarkdown?: string
  learningSessionId?: number
}>()

const emit = defineEmits<{
  (e: "close"): void
  (e: "recorded"): void
}>()

const loading = ref(false)
const requestMarkdown = ref(props.initialRequestMarkdown ?? "")
const reportMarkdown = ref("")
const rejectedEntries = ref<RejectedLearningSessionReportEntry[]>([])
const status = ref<LearningSessionCommissionResponse["status"] | "">(
  props.mode === "record"
    ? "AWAITING_REPORT"
    : props.mode === "amend"
      ? "RECORDED"
      : ""
)

const requestReady = computed(
  () =>
    props.mode === "record" ||
    props.mode === "amend" ||
    requestMarkdown.value.length > 0
)

watch(
  () => props.initialRequestMarkdown,
  (markdown) => {
    if ((props.mode === "record" || props.mode === "amend") && markdown) {
      requestMarkdown.value = markdown
    }
  },
  { immediate: true }
)

const fetchRequest = async () => {
  loading.value = true
  const { data, error } = await apiCallWithLoading(
    () =>
      LearningSessionController.request({
        query: {
          notebookId: props.notebookId,
          timezone: timezoneParam(),
        },
      }),
    { blockUi: false, message: "Loading learning session request…" }
  )
  loading.value = false

  if (error || !data) {
    return
  }

  requestMarkdown.value = data.requestMarkdown
}

onMounted(async () => {
  if (props.mode !== "record" && props.mode !== "amend") {
    await fetchRequest()
  }
})

const recordReport = async () => {
  const { data, error } = await apiCallWithLoading(
    () =>
      LearningSessionController.record({
        body: {
          notebookId: props.notebookId,
          ...(props.mode === "amend" && props.learningSessionId != null
            ? { learningSessionId: props.learningSessionId }
            : {}),
          reportMarkdown: reportMarkdown.value,
        },
        query: { timezone: timezoneParam() },
      }),
    { blockUi: true, message: "Recording learning session report…" }
  )

  if (error || !data) {
    return
  }

  rejectedEntries.value = data.rejectedEntries ?? []

  if (data.status === "RECORDED") {
    status.value = "RECORDED"
    emit("recorded")
  }
}
</script>
