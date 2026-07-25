import { PredefinedQuestionController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, vi } from "vitest"
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

  it("deletes a question when the delete button is confirmed", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true)
    const deleteQuestionSpy = mockSdkService(
      PredefinedQuestionController,
      "deleteQuestion",
      undefined
    )

    const wrapper = await mountQuestionsReady()
    await wrapper.find('button[aria-label="Delete question"]').trigger("click")
    await flushPromises()

    expect(deleteQuestionSpy).toHaveBeenCalledWith({
      path: { note: questionsNote.id, predefinedQuestion: questionsFixture[0]!.id },
    })
    expect(wrapper.text()).not.toContain("What is 2+2?")
  })

  it("does not delete the question when the confirmation is cancelled", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false)
    const deleteQuestionSpy = mockSdkService(
      PredefinedQuestionController,
      "deleteQuestion",
      undefined
    )

    const wrapper = await mountQuestionsReady()
    await wrapper.find('button[aria-label="Delete question"]').trigger("click")
    await flushPromises()

    expect(deleteQuestionSpy).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain("What is 2+2?")
  })

  it("opens the edit form prefilled with the existing question", async () => {
    await mountQuestionsReady({ attachToBody: true })
    await document.body
      .querySelector('button[aria-label="Edit question"]')
      ?.dispatchEvent(new MouseEvent("click", { bubbles: true }))
    await flushPromises()

    const stemField = document.body.querySelector(
      'textarea[name="stem"]'
    ) as HTMLTextAreaElement
    expect(stemField.value).toBe(
      questionsFixture[0]!.multipleChoicesQuestion.questionStem
    )
  })

  it("does not mutate the original question while editing before save", async () => {
    await mountQuestionsReady({ attachToBody: true })
    await document.body
      .querySelector('button[aria-label="Edit question"]')
      ?.dispatchEvent(new MouseEvent("click", { bubbles: true }))
    await flushPromises()

    const stemField = document.body.querySelector(
      'textarea[name="stem"]'
    ) as HTMLTextAreaElement
    stemField.value = "Not saved yet"
    stemField.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()

    expect(questionsFixture[0]!.multipleChoicesQuestion.questionStem).toBe(
      "What is 2+2?"
    )
  })

  it("updates the question in place when the edit form is saved", async () => {
    const updatedQuestion = {
      ...questionsFixture[0]!,
      multipleChoicesQuestion: {
        questionStem: "What is 3+3?",
        responseChoices: ["5", "6", "7", "8"],
      },
    }
    const updateQuestionSpy = mockSdkService(
      PredefinedQuestionController,
      "updateQuestion",
      updatedQuestion
    )

    const wrapper = await mountQuestionsReady({ attachToBody: true })
    await document.body
      .querySelector('button[aria-label="Edit question"]')
      ?.dispatchEvent(new MouseEvent("click", { bubbles: true }))
    await flushPromises()

    const saveButton = Array.from(
      document.body.querySelectorAll("button")
    ).find((button) => button.textContent?.trim() === "Save")
    saveButton?.dispatchEvent(new MouseEvent("click", { bubbles: true }))
    await flushPromises()

    expect(updateQuestionSpy).toHaveBeenCalledWith({
      path: {
        note: questionsNote.id,
        predefinedQuestion: questionsFixture[0]!.id,
      },
      body: expect.objectContaining({
        multipleChoicesQuestion: {
          questionStem: "What is 2+2?",
          responseChoices: ["3", "4", "5", "6"],
        },
      }),
    })
    expect(wrapper.text()).toContain("What is 3+3?")
  })
})
