import { screen } from "@testing-library/vue"
import { describe, it } from "vitest"
import AssessmentPage from "@/pages/AssessmentPage.vue"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import type { AssessmentQuestionInstance } from "@generated/backend"

vitest.mock("vue-router", () => ({
  useRouter: () => ({
    currentRoute: {
      value: {},
    },
  }),
}))

describe("assessment page", () => {
  describe("assessment with one question", () => {
    const notebook = makeMe.aNotebook.please()
    const assessmentQuestionInstance =
      makeMe.anAssessmentQuestionInstance.please()
    const assessmentAttempt = makeMe.anAssessmentAttempt
      .forNotebook(notebook)
      .withQuestions([assessmentQuestionInstance])
      .please()
    beforeEach(() => {
      vi.spyOn(
        helper.managedApi.services,
        "generateAssessmentQuestions"
      ).mockResolvedValue(assessmentAttempt as never)
    })

    it("calls API ONCE on mount", async () => {
      helper
        .component(AssessmentPage)
        .withProps({ notebookId: notebook.id })
        .render()
      expect(
        helper.managedApi.services.generateAssessmentQuestions
      ).toBeCalledTimes(1)
    })

    it("renders the questions", async () => {
      helper
        .component(AssessmentPage)
        .withProps({ notebookId: notebook.id })
        .render()
      await screen.findByText(
        assessmentQuestionInstance.multipleChoicesQuestion.stem!
      )
    })

    it("does not display score immediately after rendering", async () => {
      const { html } = helper
        .component(AssessmentPage)
        .withProps({ notebookId: notebook.id })
        .render()

      expect(html()).not.toContain("score:")
    })
  })

  describe("answering the assessment with two questions", () => {
    const notebook = makeMe.aNotebook.please()
    const quizQuestion_1 = makeMe.anAssessmentQuestionInstance
      .withChoices(["answer1", "answer2"])
      .please()
    const quizQuestion_2 = makeMe.anAssessmentQuestionInstance
      .withChoices(["answer3", "answer4"])
      .please()
    const answerResult1: AssessmentQuestionInstance = {
      ...quizQuestion_1,
      answer: { id: 1, correct: true, choiceIndex: 0 },
    }
    const answerResult2: AssessmentQuestionInstance = {
      ...quizQuestion_2,
      answer: { id: 1, correct: true, choiceIndex: 0 },
    }
    const assessmentAttempt = makeMe.anAssessmentAttempt
      .forNotebook(notebook)
      .withQuestions([quizQuestion_1, quizQuestion_2])
      .please()
    beforeEach(() => {
      vi.spyOn(
        helper.managedApi.services,
        "generateAssessmentQuestions"
      ).mockResolvedValue(assessmentAttempt as never)
      vi.spyOn(helper.managedApi.services, "answerQuestion")
        .mockResolvedValueOnce(answerResult1 as never)
        .mockResolvedValueOnce(answerResult2 as never)
      vi.spyOn(
        helper.managedApi.services,
        "submitAssessmentResult"
      ).mockResolvedValue(assessmentAttempt as never)
    })

    it("should submit assessment result when answer all questions", async () => {
      const wrapper = helper
        .component(AssessmentPage)
        .withProps({ notebookId: notebook.id })
        .render()
      await flushPromises()
      ;(await wrapper.findByRole("button", { name: "answer1" })).click()
      await flushPromises()
      ;(await wrapper.findByRole("button", { name: "answer3" })).click()
      await flushPromises()

      expect(helper.managedApi.services.submitAssessmentResult).toBeCalledWith({
        path: { assessmentAttempt: assessmentAttempt.id },
      })
    })

    it("should explicitly blur the button after clicking an answer", async () => {
      const wrapper = helper
        .component(AssessmentPage)
        .withProps({ notebookId: notebook.id })
        .render()
      await flushPromises()

      // Spy on the blur method
      const blurSpy = vi.spyOn(HTMLElement.prototype, "blur")

      const answerButton = await wrapper.findByRole("button", {
        name: "answer1",
      })
      await answerButton.click()
      await flushPromises()

      // Verify our explicit blur logic was called
      expect(blurSpy).toHaveBeenCalled()
      blurSpy.mockRestore()
    })
  })
})
