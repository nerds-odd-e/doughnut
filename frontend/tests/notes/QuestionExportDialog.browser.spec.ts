import { describe, it, vi, expect, beforeEach, afterEach } from "vitest"
import helper, { mockSdkService, wrapSdkError } from "../helpers"
import makeMe from "../fixtures/makeMe"
import QuestionExportDialog from "@/components/notes/QuestionExportDialog.vue"
import { type VueWrapper } from "@vue/test-utils"
import { reactive } from "vue"

const mockRoute = reactive({ name: "", path: "", params: {}, query: {} })
vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => mockRoute,
    useRouter: () => ({
      push: vi.fn(),
    }),
  }
})

describe("QuestionExportDialog", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
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
    const spy = mockSdkService("exportQuestionGeneration", exportData)

    wrapper = helper
      .component(QuestionExportDialog)
      .withProps({ noteId: note.id })
      .mount({ attachTo: document.body })

    await vi.waitUntil(() => {
      const el = document.body.querySelector(
        '[data-testid="export-textarea"]'
      ) as HTMLTextAreaElement
      return el && el.value.includes('"model"')
    })
    const textarea = document.body.querySelector(
      '[data-testid="export-textarea"]'
    ) as HTMLTextAreaElement
    expect(textarea).toBeTruthy()
    expect(textarea.value).toContain('"model"')
    expect(textarea.value).toContain('"title"')

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
    spy.mockResolvedValue(wrapSdkError("API Error"))

    wrapper = helper
      .component(QuestionExportDialog)
      .withProps({ noteId: note.id })
      .mount({ attachTo: document.body })

    await vi.waitUntil(() => {
      const el = document.body.querySelector(
        '[data-testid="export-textarea"]'
      ) as HTMLTextAreaElement
      return el && el.value === "Failed to load export content"
    })
    const textarea = document.body.querySelector(
      '[data-testid="export-textarea"]'
    ) as HTMLTextAreaElement
    expect(textarea.value).toBe("Failed to load export content")
  })
})
