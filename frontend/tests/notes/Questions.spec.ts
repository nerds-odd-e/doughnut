import { describe, it, vi, expect, beforeEach } from "vitest"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "../helpers"
import makeMe from "../fixtures/makeMe"
import Questions from "@/components/notes/Questions.vue"
import { fireEvent, waitFor } from "@testing-library/vue"
import { reactive } from "vue"

const mockRoute = reactive({ name: "", path: "", params: {}, query: {} })
vitest.mock("vue-router", () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

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
    mockSdkService("getAllQuestionByNote", questions)
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
    const exportQuestionGenerationSpy = mockSdkService(
      "exportQuestionGeneration",
      exportData
    )

    const { getByLabelText, getByTestId } = helper
      .component(Questions)
      .withProps({ note })
      .render()

    await flushPromises()

    const exportButton = getByLabelText("Export question generation request")
    await fireEvent.click(exportButton)

    await waitFor(() => {
      expect(getByTestId("export-textarea")).toBeTruthy()
    })

    expect(exportQuestionGenerationSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      client: expect.anything(),
    })
  })
})
