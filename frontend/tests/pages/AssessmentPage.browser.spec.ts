import { screen } from "@testing-library/vue"
import { describe, it, beforeEach, expect, vi } from "vitest"
import AssessmentPage from "@/pages/AssessmentPage.vue"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import type {
  AssessmentQuestionInstance,
  RecallPrompt,
} from "@generated/backend"

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      currentRoute: {
        value: {},
      },
    }),
  }
})

describe("assessment page", () => {
  describe("assessment with one question", () => {
    const notebook = makeMe.aNotebook.please()
    const assessmentQuestionInstance =
      makeMe.anAssessmentQuestionInstance.please()
    const recallPrompt: RecallPrompt = {
      ...assessmentQuestionInstance,
      questionType: "MCQ",
    }
    const assessmentAttempt = makeMe.anAssessmentAttempt
      .forNotebook(notebook)
      .withQuestions([recallPrompt])
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

    it("calls API ONCE on mount", () => {
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
        assessmentQuestionInstance.multipleChoicesQuestion?.f0__stem!
      )
    })

    it("does not display score immediately after rendering", () => {
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
      id: quizQuestion_1.id,
      multipleChoicesQuestion: quizQuestion_1.multipleChoicesQuestion,
      answer: { id: 1, correct: true, choiceIndex: 0 },
    }
    const answerResult2: AssessmentQuestionInstance = {
      id: quizQuestion_2.id,
      multipleChoicesQuestion: quizQuestion_2.multipleChoicesQuestion,
      answer: { id: 1, correct: true, choiceIndex: 0 },
    }
    const recallPrompt1: RecallPrompt = {
      ...quizQuestion_1,
      questionType: "MCQ",
    }
    const recallPrompt2: RecallPrompt = {
      ...quizQuestion_2,
      questionType: "MCQ",
    }
    const assessmentAttempt = makeMe.anAssessmentAttempt
      .forNotebook(notebook)
      .withQuestions([recallPrompt1, recallPrompt2])
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
      ;(await wrapper.findByText("answer1")).click()
      await flushPromises()
      ;(await wrapper.findByText("answer3")).click()
      await flushPromises()

      expect(submitAssessmentResultSpy).toBeCalledWith({
        path: { assessmentAttempt: assessmentAttempt.id },
      })
    })
  })
})
