import AssessmentPage from "@/pages/AssessmentPage.vue"
import type {
  AssessmentQuestionInstance,
  RecallPrompt,
} from "@generated/backend"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { page } from "vitest/browser"

// Helper to wait for a spy to be called (handles async timing issues)
const waitForSpy = async (
  spy: ReturnType<typeof mockSdkService>,
  timeout = 1000
) => {
  const startTime = Date.now()
  while (spy.mock.calls.length === 0 && Date.now() - startTime < timeout) {
    await new Promise((resolve) => setTimeout(resolve, 10))
    await flushPromises()
  }
}

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

      await expect
        .element(
          page.getByText(
            assessmentQuestionInstance.multipleChoicesQuestion?.f0__stem!
          )
        )
        .toBeVisible()
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
      helper
        .component(AssessmentPage)
        .withProps({ notebookId: notebook.id })
        .render()

      // Answer first question
      await page.getByText("answer1").click()

      // Wait for first answer to be processed and advance to next question
      await expect.element(page.getByText("answer3")).toBeVisible()

      // Answer second question
      await page.getByText("answer3").click()

      // Wait for the submission API call to complete
      // This handles the async timing issue where checkIfQuizComplete() is called asynchronously
      await waitForSpy(submitAssessmentResultSpy, 2000)

      expect(submitAssessmentResultSpy).toBeCalledWith({
        path: { assessmentAttempt: assessmentAttempt.id },
      })
    })
  })
})
