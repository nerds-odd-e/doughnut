import { AdminQuestionGenerationBatchController } from "@generated/doughnut-backend-api/sdk.gen"
import type { QuestionGenerationBatchAdminStatusDto } from "@generated/doughnut-backend-api/types.gen"
import { mockSdkService } from "@tests/helpers"

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
    lastMaintenanceStartedAt: undefined,
    lastMaintenanceFinishedAt: undefined,
    lastMaintenanceError: undefined,
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
    lastMaintenanceStartedAt: "2026-06-18T05:00:00.000Z",
    lastMaintenanceFinishedAt: "2026-06-18T05:01:00.000Z",
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
