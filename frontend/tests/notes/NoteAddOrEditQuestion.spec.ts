import NoteAddOrEditQuestion from "@/components/notes/NoteAddOrEditQuestion.vue"
import { userEvent } from "@testing-library/user-event"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"
import { QuizQuestionAndAnswer } from "@/generated/backend"

const note = makeMe.aNoteRealm.please()
const createWrapper = async (question?: QuizQuestionAndAnswer) => {
  helper
    .component(NoteAddOrEditQuestion)
    .withProps({
      note: note.note,
      question,
    })
    .render()
  await flushPromises()
}

describe("NoteAddOrEditQuestion", () => {
  interface Case {
    question: Record<string, string>
    questionProp?: QuizQuestionAndAnswer
    expectedRefineButton: boolean
    expectedGenerateButton: boolean
  }
  ;[
    {
      question: {} as Record<string, string>,
      expectedRefineButton: false,
      expectedGenerateButton: true,
    },
    {
      question: { Stem: "abc" },
      expectedRefineButton: true,
      expectedGenerateButton: false,
    },
    {
      question: { "Choice 1": "abc" },
      expectedRefineButton: true,
      expectedGenerateButton: false,
    },
    {
      question: {},
      questionProp: {
        id: 1234,
        quizQuestion: {
          id: 1234,
          multipleChoicesQuestion: {
            stem: "it's a me, a test",
            choices: ["mario", "luigi", "yoshi"],
          },
          correctAnswerIndex: 2,
          approved: true,
        },
      },
      expectedRefineButton: true,
      expectedGenerateButton: false,
    },
  ].forEach(async (testCase: Case) => {
    it("only allow generation when no changes", async () => {
      await createWrapper(testCase.questionProp)
      // eslint-disable-next-line no-restricted-syntax
      for (const key of Object.keys(testCase.question)) {
        // eslint-disable-next-line no-await-in-loop
        const ctrl = await screen.findByLabelText(key)
        // eslint-disable-next-line no-await-in-loop
        await userEvent.type(ctrl, testCase.question[key]!)
      }
      await flushPromises()
      const refineButton = screen.getByRole<HTMLInputElement>("button", {
        name: /refine/i,
      })
      const generateButton = screen.getByRole<HTMLInputElement>("button", {
        name: /generate/i,
      })
      expect(refineButton.disabled).toBe(!testCase.expectedRefineButton)
      expect(generateButton.disabled).toBe(!testCase.expectedGenerateButton)
    })
  })
})
