import { PredefinedQuestionController } from "@generated/doughnut-backend-api/sdk.gen"
import { describe, it, expect } from "vitest"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import {
  clickExportQuestionGeneration,
  exportTextarea,
  mountQuestionsReady,
  questionsFixture,
  questionsNote,
  sampleQuestionExportData,
  setupQuestionsTests,
} from "./questionsTestSupport"

setupQuestionsTests()

describe("Questions", () => {
  it("renders questions table when questions exist", async () => {
    const wrapper = await mountQuestionsReady()

    expect(wrapper.text()).toContain("What is 2+2?")
  })

  it("deletes a question when its delete button is clicked", async () => {
    const deleteSpy = mockSdkService(
      PredefinedQuestionController,
      "deleteQuestion",
      questionsFixture[0]!
    )

    const wrapper = await mountQuestionsReady()
    expect(wrapper.text()).toContain("What is 2+2?")

    await wrapper.find('button[aria-label="Delete question"]').trigger("click")
    await flushPromises()

    expect(deleteSpy).toHaveBeenCalledWith({
      path: { predefinedQuestion: questionsFixture[0]!.id },
    })
    expect(wrapper.text()).not.toContain("What is 2+2?")
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
})
