import { screen } from "@testing-library/vue"
import { describe, it, beforeEach } from "vitest"
import AssessmentPage from "@/pages/AssessmentPage.vue"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
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
    let generateAssessmentQuestionsSpy: ReturnType<
      typeof mockSdkService<"generateAssessmentQuestions">
    >

    beforeEach(() => {
      generateAssessmentQuestionsSpy = mockSdkService(
        "generateAssessmentQuestions",
        assessmentAttempt
      )
    })

    it("calls API ONCE on mount", async () => {
      helper
        .component(AssessmentPage)
        .withProps({ notebookId: notebook.id })
        .render()
      expect(generateAssessmentQuestionsSpy).toBeCalledTimes(1)
    })

    it("renders the questions", async () => {
      helper
        .component(AssessmentPage)
        .withProps({ notebookId: notebook.id })
        .render()
      await screen.findByText(
        assessmentQuestionInstance.multipleChoicesQuestion.f0__stem!
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
    let answerQuestionSpy: ReturnType<typeof mockSdkService<"answerQuestion">>
    let submitAssessmentResultSpy: ReturnType<
      typeof mockSdkService<"submitAssessmentResult">
    >

    beforeEach(() => {
      mockSdkService("generateAssessmentQuestions", assessmentAttempt)
      answerQuestionSpy = mockSdkService("answerQuestion", answerResult1)
      answerQuestionSpy
        .mockResolvedValueOnce(wrapSdkResponse(answerResult1))
        .mockResolvedValueOnce(wrapSdkResponse(answerResult2))
      submitAssessmentResultSpy = mockSdkService(
        "submitAssessmentResult",
        assessmentAttempt
      )
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

      expect(submitAssessmentResultSpy).toBeCalledWith({
        path: { assessmentAttempt: assessmentAttempt.id },
      })
    })
  })
})
