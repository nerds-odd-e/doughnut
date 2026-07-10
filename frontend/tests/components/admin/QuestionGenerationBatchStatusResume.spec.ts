import QuestionGenerationBatchStatus from "@/components/admin/QuestionGenerationBatchStatus.vue"
import { AdminQuestionGenerationBatchController } from "@generated/doughnut-backend-api/sdk.gen"
import type { QuestionGenerationBatchAdminStatusDto } from "@generated/doughnut-backend-api/types.gen"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
import {
  mockQuestionGenerationBatchStatusApis,
  resumedQuestionGenerationBatchStatus,
  sampleQuestionGenerationBatchStatus,
} from "./questionGenerationBatchStatusTestSupport"

describe("QuestionGenerationBatchStatus resume existing batches", () => {
  beforeEach(() => {
    mockQuestionGenerationBatchStatusApis()
  })

  it("resumes existing batches and refreshes status with summary", async () => {
    const statusSpy = mockSdkService(
      AdminQuestionGenerationBatchController,
      "getQuestionGenerationBatchStatus",
      sampleQuestionGenerationBatchStatus
    )
    const resumeSpy = mockSdkService(
      AdminQuestionGenerationBatchController,
      "resumeExistingQuestionGenerationBatches",
      resumedQuestionGenerationBatchStatus
    )
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    await flushPromises()
    const statusCallCountBeforeResume = statusSpy.mock.calls.length

    await wrapper
      .get('[data-testid="resume-existing-batches-button"]')
      .trigger("click")
    await flushPromises()

    expect(resumeSpy).toHaveBeenCalled()
    expect(statusSpy.mock.calls.length).toBeGreaterThan(
      statusCallCountBeforeResume
    )
    expect(wrapper.get('[data-testid="maintenance-summary"]').text()).toContain(
      "Batches: submitted 0, completed 6"
    )
  })

  it("disables the resume button while resuming existing batches", async () => {
    let resolveResume:
      | ((status: QuestionGenerationBatchAdminStatusDto) => void)
      | undefined
    mockSdkServiceWithImplementation(
      AdminQuestionGenerationBatchController,
      "resumeExistingQuestionGenerationBatches",
      () =>
        new Promise<QuestionGenerationBatchAdminStatusDto>((resolve) => {
          resolveResume = resolve
        })
    )
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    await flushPromises()

    await wrapper
      .get('[data-testid="resume-existing-batches-button"]')
      .trigger("click")
    await flushPromises()

    expect(
      wrapper
        .get('[data-testid="resume-existing-batches-button"]')
        .attributes("disabled")
    ).toBeDefined()

    resolveResume?.(sampleQuestionGenerationBatchStatus)
    await flushPromises()

    expect(
      wrapper
        .get('[data-testid="resume-existing-batches-button"]')
        .attributes("disabled")
    ).toBeUndefined()
  })

  it("shows a failure summary when resuming existing batches fails", async () => {
    const resumeSpy = mockSdkService(
      AdminQuestionGenerationBatchController,
      "resumeExistingQuestionGenerationBatches",
      sampleQuestionGenerationBatchStatus
    )
    resumeSpy.mockResolvedValue(wrapSdkError("resume failed"))
    const wrapper = helper.component(QuestionGenerationBatchStatus).mount()
    await flushPromises()

    await wrapper
      .get('[data-testid="resume-existing-batches-button"]')
      .trigger("click")
    await flushPromises()

    expect(wrapper.get('[data-testid="maintenance-error"]').text()).toContain(
      "Resume existing batches failed"
    )
  })
})
