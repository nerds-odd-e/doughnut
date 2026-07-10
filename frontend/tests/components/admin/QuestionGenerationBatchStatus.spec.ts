import QuestionGenerationBatchStatus from "@/components/admin/QuestionGenerationBatchStatus.vue"
import { AdminQuestionGenerationBatchController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import {
  batchStatusRowText,
  createDeferredGate,
  manualMaintenanceRunState,
  mockQuestionGenerationBatchStatusApis,
  mountQuestionGenerationBatchStatusReady,
  openaiTokenBadge,
  prodProfileBadge,
  requestStatusRowText,
  resumeExistingBatchesButton,
  sampleQuestionGenerationBatchStatus,
  scheduledMaintenanceRunState,
  schedulerBadge,
  submissionSummaryEl,
  submitRecentRecallUsersButton,
} from "./questionGenerationBatchStatusTestSupport"

describe("QuestionGenerationBatchStatus", () => {
  beforeEach(() => {
    mockQuestionGenerationBatchStatusApis()
  })

  it("displays loading message initially", () => {
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    expect(wrapper.text()).toContain(
      "Loading batch question generation status..."
    )
  })

  it("loads status and renders counts, badges, and action buttons", async () => {
    const wrapper = await mountQuestionGenerationBatchStatusReady()

    expect(wrapper.text()).toContain("PLANNED")
    expect(wrapper.text()).toContain("SUBMITTED")
    expect(wrapper.text()).toContain("PENDING")
    expect(wrapper.text()).toContain("IMPORTED")
    expect(batchStatusRowText(wrapper)).toContain("2")
    expect(requestStatusRowText(wrapper)).toContain("3")
    expect(openaiTokenBadge(wrapper).text()).toContain("configured")
    expect(schedulerBadge(wrapper).text()).toContain("not registered")
    expect(prodProfileBadge(wrapper).text()).toContain("inactive")
    expect(scheduledMaintenanceRunState(wrapper).text()).toContain(
      "Scheduled: never"
    )
    expect(manualMaintenanceRunState(wrapper).text()).toContain("Manual: never")
    expect(submitRecentRecallUsersButton(wrapper).text()).toContain(
      "Generate for recent recall users"
    )
    expect(resumeExistingBatchesButton(wrapper).text()).toContain(
      "Resume existing batches"
    )
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
    const wrapper = await mountQuestionGenerationBatchStatusReady()
    const statusCallCountBeforeSubmit = statusSpy.mock.calls.length

    await submitRecentRecallUsersButton(wrapper).trigger("click")
    await flushPromises()

    expect(submitSpy).toHaveBeenCalled()
    expect(statusSpy.mock.calls.length).toBeGreaterThan(
      statusCallCountBeforeSubmit
    )
    expect(submissionSummaryEl(wrapper).text()).toContain(
      "Considered 3, submitted 2, failed 0, skipped 1"
    )
  })

  it("disables the manual generation button while submitting", async () => {
    const { gate, resolve } = createDeferredGate()
    mockSdkServiceWithImplementation(
      AdminQuestionGenerationBatchController,
      "submitRecentRecallUsersForQuestionGenerationBatch",
      async () => {
        await gate
        return {
          consideredUserCount: 1,
          submittedCount: 1,
          failedCount: 0,
          skippedCount: 0,
        }
      }
    )
    const wrapper = await mountQuestionGenerationBatchStatusReady()

    await submitRecentRecallUsersButton(wrapper).trigger("click")
    await flushPromises()

    expect(
      submitRecentRecallUsersButton(wrapper).attributes("disabled")
    ).toBeDefined()

    resolve()
    await flushPromises()

    expect(
      submitRecentRecallUsersButton(wrapper).attributes("disabled")
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
    const wrapper = await mountQuestionGenerationBatchStatusReady()

    expect(openaiTokenBadge(wrapper).text()).toContain("not configured")
    expect(schedulerBadge(wrapper).text()).toContain("registered")
    expect(prodProfileBadge(wrapper).text()).toContain("active")
  })
})
