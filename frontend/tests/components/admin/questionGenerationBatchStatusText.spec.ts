import { describe, it, expect } from "vitest"
import {
  formatLastManualMaintenanceRun,
  formatLastScheduledMaintenanceRun,
} from "@/components/admin/questionGenerationBatchStatusText"
import type { QuestionGenerationBatchAdminStatusDto } from "@generated/donut-backend-api"

const startedAt = "2026-06-18T05:00:00.000Z"
const finishedAt = "2026-06-18T05:01:00.000Z"

describe("formatLastScheduledMaintenanceRun", () => {
  it("renders started/finished timestamps in the viewer's local time", () => {
    const status = {
      lastScheduledMaintenanceStartedAt: startedAt,
      lastScheduledMaintenanceFinishedAt: finishedAt,
    } as QuestionGenerationBatchAdminStatusDto

    const text = formatLastScheduledMaintenanceRun(status)

    expect(text).toContain(new Date(startedAt).toLocaleString())
    expect(text).toContain(new Date(finishedAt).toLocaleString())
    expect(text).not.toContain(startedAt)
    expect(text).not.toContain(finishedAt)
  })

  it("renders 'Scheduled: never' when the run has never happened", () => {
    const status = {} as QuestionGenerationBatchAdminStatusDto

    expect(formatLastScheduledMaintenanceRun(status)).toBe("Scheduled: never")
  })
})

describe("formatLastManualMaintenanceRun", () => {
  it("renders started/finished timestamps in the viewer's local time", () => {
    const status = {
      lastManualMaintenanceStartedAt: startedAt,
      lastManualMaintenanceFinishedAt: finishedAt,
    } as QuestionGenerationBatchAdminStatusDto

    const text = formatLastManualMaintenanceRun(status)

    expect(text).toContain(new Date(startedAt).toLocaleString())
    expect(text).toContain(new Date(finishedAt).toLocaleString())
    expect(text).not.toContain(startedAt)
    expect(text).not.toContain(finishedAt)
  })

  it("renders 'Manual: never' when the run has never happened", () => {
    const status = {} as QuestionGenerationBatchAdminStatusDto

    expect(formatLastManualMaintenanceRun(status)).toBe("Manual: never")
  })
})
