import { screen } from "@testing-library/vue"
import { describe, it } from "vitest"
import AssessmentPage from "@/pages/AssessmentPage.vue"
import helper from "../helpers"
import makeMe from "../fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { AnsweredQuestion } from "@/generated/backend"

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
    const quizQuestion = makeMe.aQuizQuestion.please()
    beforeEach(() => {
      helper.managedApi.restAssessmentController.generateAssessmentQuestions =
        vi.fn().mockResolvedValue([quizQuestion])
    })

    it("calls API ONCE on mount", async () => {
      helper
        .component(AssessmentPage)
        .withProps({ notebookId: notebook.id })
        .render()
      expect(
        helper.managedApi.restAssessmentController.generateAssessmentQuestions
      ).toBeCalledTimes(1)
    })

    it("renders the questions", async () => {
      helper
        .component(AssessmentPage)
        .withProps({ notebookId: notebook.id })
        .render()
      await screen.findByText(quizQuestion.multipleChoicesQuestion.stem!)
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
    const quizQuestion1 = makeMe.aQuizQuestion
      .withChoices(["answer1", "answer2"])
      .please()
    const quizQuestion2 = makeMe.aQuizQuestion
      .withChoices(["answer3", "answer4"])
      .please()
    const answerResult1: AnsweredQuestion = {
      answerId: 1,
      correct: true,
    }
    const answerResult2: AnsweredQuestion = {
      answerId: 2,
      correct: true,
    }
    beforeEach(() => {
      helper.managedApi.restAssessmentController.generateAssessmentQuestions =
        vi.fn().mockResolvedValue([quizQuestion1, quizQuestion2])
      helper.managedApi.restQuizQuestionController.answerQuiz = vi
        .fn()
        .mockResolvedValueOnce(answerResult1)
        .mockResolvedValueOnce(answerResult2)
      helper.managedApi.restAssessmentController.submitAssessmentResult = vi
        .fn()
        .mockResolvedValue({})
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

      const expectedAnswers = [
        {
          answerId: answerResult1.answerId,
          correctAnswers: true,
          questionId: quizQuestion1.id,
        },
        {
          answerId: answerResult2.answerId,
          correctAnswers: true,
          questionId: quizQuestion2.id,
        },
      ]
      expect(
        helper.managedApi.restAssessmentController.submitAssessmentResult
      ).toBeCalledWith(notebook.id, expectedAnswers)
    })
  })
})
