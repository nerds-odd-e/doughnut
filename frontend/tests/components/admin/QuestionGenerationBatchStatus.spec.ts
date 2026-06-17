import QuestionGenerationBatchStatus from "@/components/admin/QuestionGenerationBatchStatus.vue"
import { AdminQuestionGenerationBatchController } from "@generated/doughnut-backend-api/sdk.gen"
import type { QuestionGenerationBatchAdminStatusDto } from "@generated/doughnut-backend-api/types.gen"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"
import helper, { mockSdkService } from "@tests/helpers"

describe("QuestionGenerationBatchStatus", () => {
  const sampleStatus: QuestionGenerationBatchAdminStatusDto = {
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
    schedulerActive: false,
  }

  beforeEach(() => {
    mockSdkService(
      AdminQuestionGenerationBatchController,
      "getQuestionGenerationBatchStatus",
      sampleStatus
    )
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
      "inactive"
    )
  })

  it("shows warning badge when OpenAI token is not configured", async () => {
    mockSdkService(
      AdminQuestionGenerationBatchController,
      "getQuestionGenerationBatchStatus",
      {
        ...sampleStatus,
        openAiTokenConfigured: false,
        schedulerActive: true,
      }
    )
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    await flushPromises()

    expect(wrapper.get('[data-testid="openai-token-badge"]').text()).toContain(
      "not configured"
    )
    expect(wrapper.get('[data-testid="scheduler-badge"]').text()).toContain(
      "active (prod)"
    )
  })
})
