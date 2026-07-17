import NoteAddQuestion from "@/components/notes/NoteAddQuestion.vue"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { describe, it, expect } from "vitest"

const note = makeMe.aNoteRealm.please()

async function mountNoteAddQuestion() {
  helper
    .component(NoteAddQuestion)
    .withProps({
      note: note.note,
    })
    .render()
  await flushPromises()
}

function fillLabelText(label: string, value: string) {
  const ctrl = screen.getByLabelText(label) as HTMLInputElement
  ctrl.value = value
  ctrl.dispatchEvent(new Event("input", { bubbles: true }))
}

describe("NoteAddQuestion", () => {
  it.each([
    {
      case: "empty question",
      question: {} as Record<string, string>,
      expectedRefineButton: false,
      expectedGenerateButton: true,
    },
    {
      case: "stem filled",
      question: { Stem: "abc" },
      expectedRefineButton: true,
      expectedGenerateButton: false,
    },
    {
      case: "choice filled",
      question: { "Choice 1": "abc" },
      expectedRefineButton: true,
      expectedGenerateButton: false,
    },
  ])(
    "only allow generation when no changes ($case)",
    async ({ question, expectedRefineButton, expectedGenerateButton }) => {
      await mountNoteAddQuestion()
      for (const key of Object.keys(question)) {
        fillLabelText(key, question[key]!)
      }
      await flushPromises()
      const refineButton = screen.getByText(/refine/i) as HTMLButtonElement
      const generateButton = screen.getByText(
        /generate by ai/i
      ) as HTMLButtonElement
      expect(refineButton.disabled).toBe(!expectedRefineButton)
      expect(generateButton.disabled).toBe(!expectedGenerateButton)
    }
  )
})
