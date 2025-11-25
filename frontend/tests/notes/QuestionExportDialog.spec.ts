import { describe, it, vi, expect, beforeEach } from "vitest"
import helper, { mockSdkService } from "../helpers"
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
    mockSdkService("exportQuestionGeneration", exportData)

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

    const spy = mockSdkService("exportQuestionGeneration", exportData)
    expect(spy).toHaveBeenCalledWith({
      path: { note: note.id },
    })
  })

  it("displays error message when API call fails", async () => {
    const note = makeMe.aNote.please()
    const spy = mockSdkService("exportQuestionGeneration", {
      request: { model: "gpt-4", messages: [] },
      title: "Test Note",
    } as never)
    spy.mockResolvedValue({
      data: undefined,
      error: "API Error",
      request: {} as Request,
      response: {} as Response,
      // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions that require any for proper mocking
    } as any)

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
