import { describe, it, vi, expect, beforeEach } from "vitest"
import helper from "../helpers"
import makeMe from "../fixtures/makeMe"
import QuestionExportDialog from "@/components/notes/QuestionExportDialog.vue"
import { waitFor } from "@testing-library/vue"
import { reactive } from "vue"

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
    vi.spyOn(console, "error").mockImplementation(() => {
      // Suppress console.error in tests
    })
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
    vi.spyOn(
      helper.managedApi.services,
      "exportQuestionGeneration"
    ).mockResolvedValue(exportData)

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

    expect(
      helper.managedApi.services.exportQuestionGeneration
    ).toHaveBeenCalledWith({
      path: { note: note.id },
    })
  })

  it("displays error message when API call fails", async () => {
    const note = makeMe.aNote.please()
    vi.spyOn(
      helper.managedApi.services,
      "exportQuestionGeneration"
    ).mockRejectedValue(new Error("API Error"))

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
