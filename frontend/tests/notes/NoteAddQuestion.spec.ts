import NoteAddQuestion from "@/components/notes/NoteAddQuestion.vue"
import { PredefinedQuestionController } from "@generated/doughnut-backend-api/sdk.gen"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
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

  it("prefills the form and updates the question on submit when editing", async () => {
    const existingQuestion = makeMe.aPredefinedQuestion
      .withQuestionStem("Original stem?")
      .withChoices(["Right", "Wrong"])
      .correctAnswerIndex(0)
      .please()
    const updateQuestionSpy = mockSdkService(
      PredefinedQuestionController,
      "updateQuestion",
      existingQuestion
    )

    const wrapper = helper
      .component(NoteAddQuestion)
      .withProps({ note: note.note, existingQuestion })
      .mount({ attachTo: document.body })
    await flushPromises()

    expect((screen.getByLabelText("Stem") as HTMLInputElement).value).toBe(
      "Original stem?"
    )
    ;(screen.getByText(/submit/i) as HTMLButtonElement).click()
    await flushPromises()

    expect(updateQuestionSpy).toHaveBeenCalledWith({
      path: { predefinedQuestion: existingQuestion.id },
      body: existingQuestion,
    })
    expect(wrapper.emitted("close-dialog")).toBeTruthy()
    wrapper.unmount()
  })
})
