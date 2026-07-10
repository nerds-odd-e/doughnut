import QuestionGenerationBatchStatus from "@/components/admin/QuestionGenerationBatchStatus.vue"
import { AdminQuestionGenerationBatchController } from "@generated/doughnut-backend-api/sdk.gen"
import type { QuestionGenerationBatchAdminStatusDto } from "@generated/doughnut-backend-api/types.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"

export const sampleQuestionGenerationBatchStatus: QuestionGenerationBatchAdminStatusDto =
  {
    batchCountsByStatus: {
      PLANNED: 2,
      SUBMITTED: 1,
      COMPLETED: 5,
      FAILED: 0,
      EXPIRED: 0,
    },
    requestCountsByStatus: {
      PENDING: 3,
      OUTPUT_READY: 1,
      FAILED: 0,
      IMPORTED: 10,
    },
    openAiTokenConfigured: true,
    prodProfileActive: false,
    schedulerActive: false,
    lastScheduledMaintenanceStartedAt: undefined,
    lastScheduledMaintenanceFinishedAt: undefined,
    lastScheduledMaintenanceError: undefined,
    lastManualMaintenanceStartedAt: undefined,
    lastManualMaintenanceFinishedAt: undefined,
    lastManualMaintenanceError: undefined,
  }

export const resumedQuestionGenerationBatchStatus: QuestionGenerationBatchAdminStatusDto =
  {
    ...sampleQuestionGenerationBatchStatus,
    batchCountsByStatus: {
      ...sampleQuestionGenerationBatchStatus.batchCountsByStatus,
      SUBMITTED: 0,
      COMPLETED: 6,
    },
    requestCountsByStatus: {
      ...sampleQuestionGenerationBatchStatus.requestCountsByStatus,
      PENDING: 0,
      OUTPUT_READY: 0,
      IMPORTED: 13,
    },
    lastManualMaintenanceStartedAt: "2026-06-18T05:00:00.000Z",
    lastManualMaintenanceFinishedAt: "2026-06-18T05:01:00.000Z",
  }

export const mockQuestionGenerationBatchStatusApis = () => {
  mockSdkService(
    AdminQuestionGenerationBatchController,
    "getQuestionGenerationBatchStatus",
    sampleQuestionGenerationBatchStatus
  )
  mockSdkService(
    AdminQuestionGenerationBatchController,
    "submitRecentRecallUsersForQuestionGenerationBatch",
    {
      consideredUserCount: 3,
      submittedCount: 2,
      failedCount: 0,
      skippedCount: 1,
    }
  )
  mockSdkService(
    AdminQuestionGenerationBatchController,
    "resumeExistingQuestionGenerationBatches",
    resumedQuestionGenerationBatchStatus
  )
}

export function createDeferredGate() {
  let resolveGate!: () => void
  const gate = new Promise<void>((resolve) => {
    resolveGate = resolve
  })
  return { gate, resolve: () => resolveGate() }
}

export async function mountQuestionGenerationBatchStatusReady() {
  const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
  await flushPromises()
  return wrapper
}

export function submitRecentRecallUsersButton(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="submit-recent-recall-users-button"]')
}

export function resumeExistingBatchesButton(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="resume-existing-batches-button"]')
}

export function submissionSummaryEl(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="submission-summary"]')
}

export function openaiTokenBadge(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="openai-token-badge"]')
}

export function schedulerBadge(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="scheduler-badge"]')
}

export function prodProfileBadge(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="prod-profile-badge"]')
}

export function scheduledMaintenanceRunState(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="scheduled-maintenance-run-state"]')
}

export function manualMaintenanceRunState(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="manual-maintenance-run-state"]')
}

export function batchStatusRowText(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="batch-status-row"]').text()
}

export function requestStatusRowText(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="request-status-row"]').text()
}
