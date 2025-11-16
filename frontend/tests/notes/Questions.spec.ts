import { describe, it, vi, expect, beforeEach } from "vitest"
import { flushPromises } from "@vue/test-utils"
import helper from "../helpers"
import makeMe from "../fixtures/makeMe"
import Questions from "@/components/notes/Questions.vue"
import { fireEvent, waitFor } from "@testing-library/vue"

describe("Questions", () => {
  const note = makeMe.aNote.please()
  const questions = [
    makeMe.aPredefinedQuestion
      .withQuestionStem("What is 2+2?")
      .withChoices(["3", "4", "5", "6"])
      .correctAnswerIndex(1)
      .please(),
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(
      helper.managedApi.services,
      "getAllQuestionByNote"
    ).mockResolvedValue(questions as never)
  })

  it("renders questions table when questions exist", async () => {
    const { getByText } = helper
      .component(Questions)
      .withProps({ note })
      .render()

    await waitFor(() => {
      expect(getByText("What is 2+2?")).toBeTruthy()
    })
  })

  it("shows export dialog when export button is clicked", async () => {
    const exportData = {
      request: {
        model: "gpt-4",
        messages: [],
      },
      title: "Test Note",
    } as never
    vi.spyOn(
      helper.managedApi.services,
      "exportQuestionGeneration"
    ).mockResolvedValue(exportData)

    const { getByLabelText, getByTestId } = helper
      .component(Questions)
      .withProps({ note })
      .withRouter()
      .render()

    await flushPromises()

    const exportButton = getByLabelText("Export question generation request")
    await fireEvent.click(exportButton)

    await waitFor(() => {
      expect(getByTestId("export-textarea")).toBeTruthy()
    })

    expect(
      helper.managedApi.services.exportQuestionGeneration
    ).toHaveBeenCalledWith({
      note: note.id,
    })
  })
})
