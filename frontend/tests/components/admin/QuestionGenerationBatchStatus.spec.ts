import QuestionGenerationBatchStatus from "@/components/admin/QuestionGenerationBatchStatus.vue"
import { AdminQuestionGenerationBatchController } from "@generated/doughnut-backend-api/sdk.gen"
import type { QuestionGenerationBatchSubmissionSummaryDto } from "@generated/doughnut-backend-api/types.gen"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import {
  mockQuestionGenerationBatchStatusApis,
  sampleQuestionGenerationBatchStatus,
} from "./questionGenerationBatchStatusTestHelper"

describe("QuestionGenerationBatchStatus", () => {
  beforeEach(() => {
    mockQuestionGenerationBatchStatusApis()
  })

  it("displays loading message initially", async () => {
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    expect(wrapper.text()).toContain(
      "Loading batch question generation status..."
    )
  })

  it("displays batch and request status counts after loading", async () => {
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    await flushPromises()

    expect(wrapper.text()).toContain("PLANNED")
    expect(wrapper.text()).toContain("SUBMITTED")
    expect(wrapper.text()).toContain("PENDING")
    expect(wrapper.text()).toContain("IMPORTED")
    expect(wrapper.get('[data-testid="batch-status-row"]').text()).toContain(
      "2"
    )
    expect(wrapper.get('[data-testid="request-status-row"]').text()).toContain(
      "3"
    )
  })

  it("displays availability badges", async () => {
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    await flushPromises()

    expect(wrapper.get('[data-testid="openai-token-badge"]').text()).toContain(
      "configured"
    )
    expect(wrapper.get('[data-testid="scheduler-badge"]').text()).toContain(
      "not registered"
    )
    expect(wrapper.get('[data-testid="prod-profile-badge"]').text()).toContain(
      "inactive"
    )
    expect(
      wrapper.get('[data-testid="scheduled-maintenance-run-state"]').text()
    ).toContain("Scheduled: never")
    expect(
      wrapper.get('[data-testid="manual-maintenance-run-state"]').text()
    ).toContain("Manual: never")
  })

  it("renders the manual generation button", async () => {
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    await flushPromises()

    expect(
      wrapper.get('[data-testid="submit-recent-recall-users-button"]').text()
    ).toContain("Generate for recent recall users")
  })

  it("renders the resume existing batches button", async () => {
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    await flushPromises()

    expect(
      wrapper.get('[data-testid="resume-existing-batches-button"]').text()
    ).toContain("Resume existing batches")
  })

  it("triggers manual generation and refreshes status with summary", async () => {
    const statusSpy = mockSdkService(
      AdminQuestionGenerationBatchController,
      "getQuestionGenerationBatchStatus",
      sampleQuestionGenerationBatchStatus
    )
    const submitSpy = mockSdkService(
      AdminQuestionGenerationBatchController,
      "submitRecentRecallUsersForQuestionGenerationBatch",
      {
        consideredUserCount: 3,
        submittedCount: 2,
        failedCount: 0,
        skippedCount: 1,
      }
    )
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    await flushPromises()
    const statusCallCountBeforeSubmit = statusSpy.mock.calls.length

    await wrapper
      .get('[data-testid="submit-recent-recall-users-button"]')
      .trigger("click")
    await flushPromises()

    expect(submitSpy).toHaveBeenCalled()
    expect(statusSpy.mock.calls.length).toBeGreaterThan(
      statusCallCountBeforeSubmit
    )
    expect(wrapper.get('[data-testid="submission-summary"]').text()).toContain(
      "Considered 3, submitted 2, failed 0, skipped 1"
    )
  })

  it("disables the manual generation button while submitting", async () => {
    let resolveSubmission:
      | ((summary: QuestionGenerationBatchSubmissionSummaryDto) => void)
      | undefined
    mockSdkServiceWithImplementation(
      AdminQuestionGenerationBatchController,
      "submitRecentRecallUsersForQuestionGenerationBatch",
      () =>
        new Promise<QuestionGenerationBatchSubmissionSummaryDto>((resolve) => {
          resolveSubmission = resolve
        })
    )
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    await flushPromises()

    await wrapper
      .get('[data-testid="submit-recent-recall-users-button"]')
      .trigger("click")
    await flushPromises()

    expect(
      wrapper
        .get('[data-testid="submit-recent-recall-users-button"]')
        .attributes("disabled")
    ).toBeDefined()

    resolveSubmission?.({
      consideredUserCount: 1,
      submittedCount: 1,
      failedCount: 0,
      skippedCount: 0,
    })
    await flushPromises()

    expect(
      wrapper
        .get('[data-testid="submit-recent-recall-users-button"]')
        .attributes("disabled")
    ).toBeUndefined()
  })

  it("shows warning badge when OpenAI token is not configured", async () => {
    mockSdkService(
      AdminQuestionGenerationBatchController,
      "getQuestionGenerationBatchStatus",
      {
        ...sampleQuestionGenerationBatchStatus,
        openAiTokenConfigured: false,
        prodProfileActive: true,
        schedulerActive: true,
      }
    )
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    await flushPromises()

    expect(wrapper.get('[data-testid="openai-token-badge"]').text()).toContain(
      "not configured"
    )
    expect(wrapper.get('[data-testid="scheduler-badge"]').text()).toContain(
      "registered"
    )
    expect(wrapper.get('[data-testid="prod-profile-badge"]').text()).toContain(
      "active"
    )
  })
})
