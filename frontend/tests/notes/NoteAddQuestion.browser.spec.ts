import NoteAddQuestion from "@/components/notes/NoteAddQuestion.vue"
import { userEvent } from "@testing-library/user-event"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { describe, it, expect } from "vitest"

const note = makeMe.aNoteRealm.please()
const createWrapper = async () => {
  helper
    .component(NoteAddQuestion)
    .withProps({
      note: note.note,
    })
    .render()
  await flushPromises()
}

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
      await createWrapper()
      // eslint-disable-next-line no-restricted-syntax
      for (const key of Object.keys(testCase.question)) {
        // eslint-disable-next-line no-await-in-loop
        const ctrl = await screen.findByLabelText(key)
        // eslint-disable-next-line no-await-in-loop
        await userEvent.type(ctrl, testCase.question[key]!)
      }
      await flushPromises()
      const refineButton = screen.getByText(/refine/i) as HTMLButtonElement
      const generateButton = screen.getByText(
        /generate by ai/i
      ) as HTMLButtonElement
      expect(refineButton.disabled).toBe(!testCase.expectedRefineButton)
      expect(generateButton.disabled).toBe(!testCase.expectedGenerateButton)
    })
  })
})
