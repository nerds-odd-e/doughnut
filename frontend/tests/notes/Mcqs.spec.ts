import { McqController } from "@generated/donut-backend-api/sdk.gen"
import { describe, it, expect } from "vitest"
import { mockSdkService } from "@tests/helpers"
import {
  clickExportQuestionGeneration,
  exportTextarea,
  mountMcqsReady,
  mcqsNote,
  sampleMcqExportData,
  setupMcqsTests,
} from "./mcqsTestSupport"

setupMcqsTests()

describe("Mcqs", () => {
  it("renders questions table when questions exist", async () => {
    const wrapper = await mountMcqsReady()

    expect(wrapper.find(".mcq-table").text()).toContain("What is 2+2?")
  })

  it("shows export dialog when export button is clicked", async () => {
    const exportSpy = mockSdkService(
      McqController,
      "export",
      sampleMcqExportData
    )

    await mountMcqsReady({ attachToBody: true })
    await clickExportQuestionGeneration()

    expect(exportTextarea()).toBeTruthy()
    expect(exportSpy).toHaveBeenCalledWith({
      path: { note: mcqsNote.id },
    })
  })
})
