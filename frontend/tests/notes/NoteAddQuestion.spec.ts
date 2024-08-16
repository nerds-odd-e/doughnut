import NoteAddQuestion from "@/components/notes/NoteAddQuestion.vue"
import { userEvent } from "@testing-library/user-event"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

const note = makeMe.aNoteRealm.please()

describe("NoteAddQuestion", () => {
  interface Case {
    question: Record<string, string>
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
  ].forEach(async (testCase: Case) => {
    it("only allow generation when no changes", async () => {
      helper
        .component(NoteAddQuestion)
        .withProps({
          note: note.note,
        })
        .render()
      await flushPromises()
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

  it("turns into edit dialog when question is provided", async () => {
    const wrapper = helper
      .component(NoteAddQuestion)
      .withProps({
        note: note.note,
        question: makeMe.aQuizQuestionAndAnswer
          .withQuestionStem('Which of the following is the letter "A"?')
          .withChoices(["A", "B"])
          .please(),
      })
      .render()
    await flushPromises()
    expect(
      wrapper.getByDisplayValue('Which of the following is the letter "A"?')
    ).toHaveAttribute("name", "stem")
    expect(wrapper.getByDisplayValue("A")).toHaveAttribute("name", "choice 0")
    expect(wrapper.getByDisplayValue("B")).toHaveAttribute("name", "choice 1")
  })
})
