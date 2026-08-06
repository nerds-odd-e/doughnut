import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import {
  createClipboardEvent,
  emitRichEditorPasteComplete,
  mountNoteEditableContent,
  setupPopupsMock,
  setupUpdateNoteContentMock,
  textareaEl,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

describe("NoteEditableContent paste", () => {
  // biome-ignore lint/suspicious/noExplicitAny: Mock type for testing
  let mockPopupsOptions: any

  beforeEach(() => {
    vi.resetAllMocks()
    setupUpdateNoteContentMock()
    mockPopupsOptions = vi.fn().mockResolvedValue(null)
    setupPopupsMock(mockPopupsOptions)
  })

  afterEach(() => {
    document.body.innerHTML = ""
  })

  it("converts HTML to markdown when pasting HTML content", async () => {
    const wrapper = mountNoteEditableContent(
      { noteId: 1, noteContent: "existing text" },
      { attachTo: document.body }
    )
    await flushPromises()

    const textarea = textareaEl(wrapper)
    textarea.setSelectionRange(8, 8)

    await textarea.dispatchEvent(
      createClipboardEvent("<p><strong>Bold text</strong></p>")
    )
    await flushPromises()

    expect(textarea.value).toContain("Bold text")
    expect(textarea.value).toContain("existing")
    wrapper.unmount()
  })

  describe("textarea", () => {
    it("shows options popup when pasted content contains links", async () => {
      const wrapper = mountNoteEditableContent(
        { noteId: 1, noteContent: "" },
        { attachTo: document.body }
      )
      await flushPromises()

      await textareaEl(wrapper).dispatchEvent(
        createClipboardEvent(
          '<p>Check <a href="https://example.com">this link</a></p>'
        )
      )
      await flushPromises()

      expect(mockPopupsOptions).toHaveBeenCalledWith(
        "The content contains 1 links.",
        expect.arrayContaining([{ label: "Remove 1 links", value: "links" }])
      )
      wrapper.unmount()
    })

    it("does not show popup when pasted content has no links or images", async () => {
      const wrapper = mountNoteEditableContent(
        { noteId: 1, noteContent: "plain text" },
        { attachTo: document.body }
      )
      await flushPromises()

      await textareaEl(wrapper).dispatchEvent(
        createClipboardEvent("<p><strong>Bold text</strong></p>")
      )
      await flushPromises()

      expect(mockPopupsOptions).not.toHaveBeenCalled()
      wrapper.unmount()
    })

    it("removes all links from entire content when user selects remove links", async () => {
      mockPopupsOptions.mockResolvedValue("links")

      const wrapper = mountNoteEditableContent(
        {
          noteId: 1,
          noteContent: "[existing link](https://existing.com) ",
        },
        { attachTo: document.body }
      )
      await flushPromises()

      const textarea = textareaEl(wrapper)
      await textarea.dispatchEvent(
        createClipboardEvent(
          '<p><a href="https://example.com">new link</a></p>'
        )
      )
      await flushPromises()

      expect(textarea.value).toContain("existing link")
      expect(textarea.value).toContain("new link")
      expect(textarea.value).not.toContain("https://existing.com")
      expect(textarea.value).not.toContain("https://example.com")
      wrapper.unmount()
    })
  })

  describe("quill editor", () => {
    function mountRichEditor(noteContent: string) {
      return mountNoteEditableContent(
        { noteId: 1, noteContent, asMarkdown: false },
        { attachTo: document.body }
      )
    }

    it("shows options popup based on content after paste", async () => {
      const wrapper = mountRichEditor("plain text")
      await flushPromises()

      emitRichEditorPasteComplete(
        wrapper,
        "plain text [new link](https://example.com)"
      )
      await flushPromises()

      expect(mockPopupsOptions).toHaveBeenCalledWith(
        "The content contains 1 links.",
        expect.arrayContaining([{ label: "Remove 1 links", value: "links" }])
      )
      wrapper.unmount()
    })

    it("does not show popup when quill content has no links or images", async () => {
      const wrapper = mountRichEditor("plain text")
      await flushPromises()

      emitRichEditorPasteComplete(wrapper, "plain text with more plain text")
      await flushPromises()

      expect(mockPopupsOptions).not.toHaveBeenCalled()
      wrapper.unmount()
    })
  })
})
