import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import {
  createClipboardEvent,
  mountNoteEditableContent,
  setupPopupsMock,
  setupUpdateNoteContentMock,
  textareaEl,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

// The pending paste choice from NoteEditableContent.pasteChoice.spec.ts and the
// pre-existing whole-note link/image removal modal (NoteEditableContent.paste.spec.ts)
// can both be triggered by the same paste; this file covers only their coexistence.
describe("NoteEditableContent paste choice link removal prompt coexistence", () => {
  // biome-ignore lint/suspicious/noExplicitAny: Mock type for testing
  let popupsOptionsMock: any

  beforeEach(() => {
    vi.resetAllMocks()
    setupUpdateNoteContentMock()
    popupsOptionsMock = vi.fn().mockResolvedValue(null)
    setupPopupsMock(popupsOptionsMock)
  })

  afterEach(() => {
    document.body.innerHTML = ""
  })

  const linkHtml = '<p><a href="https://example.com">new link</a></p>'
  const rawOriginal = "RAW original text"

  async function mountAndPasteLinkHtml() {
    const wrapper = mountNoteEditableContent(
      { noteId: 1, noteContent: "" },
      { attachTo: document.body }
    )
    await flushPromises()
    const textarea = textareaEl(wrapper)

    await textarea.dispatchEvent(createClipboardEvent(linkHtml, rawOriginal))
    await flushPromises()

    expect(popupsOptionsMock).toHaveBeenCalled()
    return { wrapper, textarea }
  }

  it("resumes the paste choice after the removal prompt is cancelled", async () => {
    const { wrapper, textarea } = await mountAndPasteLinkHtml()

    expect(wrapper.find('[data-testid="paste-choice-action"]').exists()).toBe(
      true
    )

    await wrapper.find('[data-testid="paste-choice-action"]').trigger("click")
    await flushPromises()

    expect(textarea.value).toBe(rawOriginal)
    wrapper.unmount()
  })

  it("consumes the paste choice when the removal prompt's removal is applied", async () => {
    popupsOptionsMock.mockResolvedValue("links")

    const { wrapper, textarea } = await mountAndPasteLinkHtml()

    expect(wrapper.find('[data-testid="paste-choice"]').exists()).toBe(false)
    expect(textarea.value).toContain("new link")
    expect(textarea.value).not.toContain("https://example.com")
    expect(textarea.value).not.toContain(rawOriginal)
    wrapper.unmount()
  })

  it("does not reopen the removal prompt when the alternative is applied", async () => {
    const convertedHtml = "<p>Styled text</p>"
    const originalMarkdown = "**RAW markdown**"
    const wrapper = mountNoteEditableContent(
      { noteId: 1, noteContent: "" },
      { attachTo: document.body }
    )
    await flushPromises()
    await textareaEl(wrapper).dispatchEvent(
      createClipboardEvent(convertedHtml, originalMarkdown)
    )
    await flushPromises()
    expect(popupsOptionsMock).not.toHaveBeenCalled()

    await wrapper.find('[data-testid="paste-choice-action"]').trigger("click")
    await flushPromises()

    expect(popupsOptionsMock).not.toHaveBeenCalled()
    wrapper.unmount()
  })
})
