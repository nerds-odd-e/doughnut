import { PredefinedQuestionController } from "@generated/doughnut-backend-api/sdk.gen"
import { describe, it, expect, vi } from "vitest"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import {
  clickExportQuestionGeneration,
  exportTextarea,
  mountQuestionsReady,
  questionsNote,
  sampleQuestionExportData,
  setupQuestionsTests,
  wrapper,
} from "./questionsTestSupport"

const showSuccessToast = vi.fn()

vi.mock("@/composables/useToast", () => ({
  useToast: () => ({
    showSuccessToast,
    showErrorToast: vi.fn(),
  }),
}))

setupQuestionsTests()

describe("Questions", () => {
  it("renders questions table when questions exist", async () => {
    const mounted = await mountQuestionsReady()

    expect(mounted.text()).toContain("What is 2+2?")
  })

  it("shows export dialog when export button is clicked", async () => {
    const exportQuestionGenerationSpy = mockSdkService(
      PredefinedQuestionController,
      "exportQuestionGeneration",
      sampleQuestionExportData
    )

    await mountQuestionsReady({ attachToBody: true })
    await clickExportQuestionGeneration()

    expect(exportTextarea()).toBeTruthy()
    expect(exportQuestionGenerationSpy).toHaveBeenCalledWith({
      path: { note: questionsNote.id },
    })
  })

  it("shows delete success toast after confirming deletion", async () => {
    showSuccessToast.mockClear()
    mockSdkService(PredefinedQuestionController, "deleteQuestions", undefined)
    await mountQuestionsReady({ attachToBody: true })

    await wrapper
      .find(`input[aria-label="Select question What is 2+2?"]`)
      .setValue(true)
    await wrapper.find('button[aria-label="Delete Question"]').trigger("click")
    await wrapper.find('button[aria-label="Confirm"]').trigger("click")
    await flushPromises()

    expect(showSuccessToast).toHaveBeenCalledWith("Delete success")
    expect(wrapper.text()).not.toContain("What is 2+2?")
  })

  it("keeps questions when delete fails", async () => {
    showSuccessToast.mockClear()
    const deleteSpy = mockSdkService(
      PredefinedQuestionController,
      "deleteQuestions",
      undefined
    )
    deleteSpy.mockResolvedValue(
      wrapSdkError({
        message:
          "Delete failed: One or more questions do not belong to this note.",
      })
    )
    await mountQuestionsReady({ attachToBody: true })

    await wrapper
      .find(`input[aria-label="Select question What is 2+2?"]`)
      .setValue(true)
    await wrapper.find('button[aria-label="Delete Question"]').trigger("click")
    await wrapper.find('button[aria-label="Confirm"]').trigger("click")
    await flushPromises()

    expect(showSuccessToast).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain("What is 2+2?")
  })
})
