import { PredefinedQuestionController } from "@generated/doughnut-backend-api/sdk.gen"
import { describe, it, expect } from "vitest"
import { mockSdkService } from "@tests/helpers"
import {
  clickExportQuestionGeneration,
  exportTextarea,
  mountQuestionsReady,
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
