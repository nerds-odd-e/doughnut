import { describe, it, vi, expect, beforeEach, afterEach } from "vitest"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import NoteExportForm from "@/components/notes/core/NoteExportForm.vue"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { saveAs } from "file-saver"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { Note } from "@generated/donut-backend-api"

vi.mock("file-saver", () => ({ saveAs: vi.fn() }))

const aiMarkdownStub = { markdown: "# AI context\n\nHello **world**." }

describe("NoteExportForm", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    vi.clearAllMocks()
    mockSdkService(NoteController, "getAiContextMarkdown", aiMarkdownStub)
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  const mountForm = (note: Note = makeMe.aNote.please()) => {
    wrapper = helper
      .component(NoteExportForm)
      .withProps({ note })
      .mount({ attachTo: document.body })
    return note
  }

  const aiMarkdownTextarea = () =>
    document.querySelector(
      '[data-testid="ai-context-markdown-textarea"]'
    ) as HTMLTextAreaElement | null

  const clickTestId = async (testId: string) => {
    await wrapper.find(`[data-testid="${testId}"]`).trigger("click")
    await flushPromises()
  }

  const setTokenLimit = async (value: string) => {
    const input = wrapper.find('[data-testid="token-limit-input"]')
    await input.setValue(value)
    await flushPromises()
  }

  it("fetches AI markdown on open and downloads from primary button", async () => {
    const note = mountForm()
    await flushPromises()

    expect(NoteController.getAiContextMarkdown).toHaveBeenCalledWith({
      path: { note: note.id },
      query: { tokenLimit: 2000 },
    })

    expect(aiMarkdownTextarea()?.value).toContain("AI context")

    await clickTestId("download-ai-context-md-btn")
    expect(saveAs).toHaveBeenCalled()
    const blobArg = vi.mocked(saveAs).mock.calls[0][0] as Blob
    expect(blobArg.type).toContain("markdown")
  })

  it("refresh markdown refetches with current token budget", async () => {
    const note = makeMe.aNote.please()
    const md1 = { markdown: "first" }
    const md2 = { markdown: "second-budget" }
    vi.mocked(NoteController.getAiContextMarkdown).mockReset()
    vi.mocked(NoteController.getAiContextMarkdown)
      .mockResolvedValueOnce(wrapSdkResponse(md1))
      .mockResolvedValueOnce(wrapSdkResponse(md2))
    mountForm(note)
    await flushPromises()

    await setTokenLimit("3000")
    await clickTestId("refresh-context-md-btn")

    expect(aiMarkdownTextarea()?.value).toContain("second-budget")
    expect(NoteController.getAiContextMarkdown).toHaveBeenLastCalledWith({
      path: { note: note.id },
      query: { tokenLimit: 3000 },
    })
  })
})
