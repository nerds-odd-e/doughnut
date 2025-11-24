import { describe, it, vi, expect, beforeEach } from "vitest"
import helper from "../helpers"
import makeMe from "../fixtures/makeMe"
import QuestionExportDialog from "@/components/notes/QuestionExportDialog.vue"
import { waitFor } from "@testing-library/vue"
import { reactive } from "vue"
import { PredefinedQuestionController } from "@generated/backend/sdk.gen"

const mockRoute = reactive({ name: "", path: "", params: {}, query: {} })
vitest.mock("vue-router", () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

describe("QuestionExportDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("fetches and displays export content", async () => {
    const note = makeMe.aNote.please()
    const exportData = {
      request: {
        model: "gpt-4",
        messages: [{ role: "system", content: "test" }],
      },
      title: "Test Note",
    } as never
    vi.spyOn(PredefinedQuestionController, "exportQuestionGeneration").mockResolvedValue({
      data: exportData,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })

    const { getByTestId } = helper
      .component(QuestionExportDialog)
      .withProps({ noteId: note.id })
      .render()

    await waitFor(() => {
      const textarea = getByTestId("export-textarea") as HTMLTextAreaElement
      expect(textarea).toBeTruthy()
      expect(textarea.value).toContain('"model"')
      expect(textarea.value).toContain('"title"')
    })

    expect(PredefinedQuestionController.exportQuestionGeneration).toHaveBeenCalledWith({
      path: { note: note.id },
    })
  })

  it("displays error message when API call fails", async () => {
    const note = makeMe.aNote.please()
    vi.spyOn(PredefinedQuestionController, "exportQuestionGeneration").mockResolvedValue({
      data: undefined,
      error: "API Error",
      request: {} as Request,
      response: {} as Response,
    })

    const { getByTestId } = helper
      .component(QuestionExportDialog)
      .withProps({ noteId: note.id })
      .render()

    await waitFor(() => {
      const textarea = getByTestId("export-textarea") as HTMLTextAreaElement
      expect(textarea.value).toBe("Failed to load export content")
    })
  })
})
