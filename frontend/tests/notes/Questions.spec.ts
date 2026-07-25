import { PredefinedQuestionController } from "@generated/doughnut-backend-api/sdk.gen"
import { describe, it, expect, vi } from "vitest"
import { flushPromises } from "@vue/test-utils"
import { screen } from "@testing-library/vue"
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

  it("deletes a question after confirmation", async () => {
    const deleteQuestionSpy = mockSdkService(
      PredefinedQuestionController,
      "deleteQuestion",
      undefined
    )
    vi.spyOn(window, "confirm").mockReturnValue(true)

    const wrapper = await mountQuestionsReady()
    expect(wrapper.text()).toContain("What is 2+2?")

    await wrapper.find('button[aria-label="Delete question"]').trigger("click")
    await flushPromises()

    expect(deleteQuestionSpy).toHaveBeenCalledWith({
      path: { predefinedQuestion: questionsFixture[0]!.id },
    })
    expect(wrapper.text()).not.toContain("What is 2+2?")
  })

  it("does not delete a question when confirmation is cancelled", async () => {
    const deleteQuestionSpy = mockSdkService(
      PredefinedQuestionController,
      "deleteQuestion",
      undefined
    )
    vi.spyOn(window, "confirm").mockReturnValue(false)

    const wrapper = await mountQuestionsReady()

    await wrapper.find('button[aria-label="Delete question"]').trigger("click")
    await flushPromises()

    expect(deleteQuestionSpy).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain("What is 2+2?")
  })

  it("opens a prefilled edit form for a question", async () => {
    const wrapper = await mountQuestionsReady({ attachToBody: true })

    await wrapper.find('button[aria-label="Edit question"]').trigger("click")
    await flushPromises()

    expect((screen.getByLabelText("Stem") as HTMLInputElement).value).toBe(
      "What is 2+2?"
    )
  })
})
