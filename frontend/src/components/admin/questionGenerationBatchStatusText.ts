import type {
  QuestionGenerationBatchAdminStatusDto,
  QuestionGenerationBatchSubmissionSummaryDto,
} from "@generated/doughnut-backend-api"

export const formatSubmissionSummary = (
  summary: QuestionGenerationBatchSubmissionSummaryDto | undefined
): string | undefined => {
  if (!summary) return
  return [
    `Considered ${summary.consideredUserCount ?? 0}`,
    `submitted ${summary.submittedCount ?? 0}`,
    `failed ${summary.failedCount ?? 0}`,
    `skipped ${summary.skippedCount ?? 0}`,
  ].join(", ")
}

export const formatMaintenanceSummary = (
  summary: QuestionGenerationBatchAdminStatusDto | undefined
): string | undefined => {
  if (!summary) return
  return [
    `Batches: submitted ${summary.batchCountsByStatus?.SUBMITTED ?? 0}`,
    `completed ${summary.batchCountsByStatus?.COMPLETED ?? 0}`,
    `Requests: pending ${summary.requestCountsByStatus?.PENDING ?? 0}`,
    `output ready ${summary.requestCountsByStatus?.OUTPUT_READY ?? 0}`,
    `imported ${summary.requestCountsByStatus?.IMPORTED ?? 0}`,
    `failed ${summary.requestCountsByStatus?.FAILED ?? 0}`,
  ].join(", ")
}

export const formatLastMaintenanceRun = (
  status: QuestionGenerationBatchAdminStatusDto | undefined
): string => {
  const startedAt = status?.lastMaintenanceStartedAt
  const finishedAt = status?.lastMaintenanceFinishedAt
  const error = status?.lastMaintenanceError
  if (!startedAt && !finishedAt) return "never"
  return [
    startedAt ? `started ${startedAt}` : undefined,
    finishedAt ? `finished ${finishedAt}` : undefined,
    error ? `error ${error}` : undefined,
  ]
    .filter(Boolean)
    .join(", ")
}
