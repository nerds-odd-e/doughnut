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
    it.each([
      {
        name: "shows options popup when content contains links after paste",
        initialContent: "",
        pasteHtml: '<p>Check <a href="https://example.com">this link</a></p>',
        expectPopup: true,
      },
      {
        name: "does not show popup when entire content has no links or images",
        initialContent: "plain text",
        pasteHtml: "<p><strong>Bold text</strong></p>",
        expectPopup: false,
      },
    ])("$name", async ({ initialContent, pasteHtml, expectPopup }) => {
      const wrapper = mountNoteEditableContent(
        { noteId: 1, noteContent: initialContent },
        { attachTo: document.body }
      )
      await flushPromises()

      await textareaEl(wrapper).dispatchEvent(createClipboardEvent(pasteHtml))
      await flushPromises()

      if (expectPopup) {
        expect(mockPopupsOptions).toHaveBeenCalledWith(
          "The content contains 1 links.",
          expect.arrayContaining([{ label: "Remove 1 links", value: "links" }])
        )
      } else {
        expect(mockPopupsOptions).not.toHaveBeenCalled()
      }
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

    function emitPaste(
      wrapper: ReturnType<typeof mountRichEditor>,
      newContent: string
    ) {
      const richEditor = wrapper.findComponent({ name: "RichMarkdownEditor" })
      richEditor.vm.$emit("update:modelValue", newContent)
      richEditor.vm.$emit("pasteComplete", newContent)
    }

    it.each([
      {
        name: "shows options popup based on content AFTER paste, not before",
        newContent: "plain text [new link](https://example.com)",
        expectPopup: true,
      },
      {
        name: "does not show popup when quill content has no links or images",
        newContent: "plain text with more plain text",
        expectPopup: false,
      },
    ])("$name", async ({ newContent, expectPopup }) => {
      const wrapper = mountRichEditor("plain text")
      await flushPromises()

      emitPaste(wrapper, newContent)
      await flushPromises()

      if (expectPopup) {
        expect(mockPopupsOptions).toHaveBeenCalledWith(
          "The content contains 1 links.",
          expect.arrayContaining([{ label: "Remove 1 links", value: "links" }])
        )
      } else {
        expect(mockPopupsOptions).not.toHaveBeenCalled()
      }
      wrapper.unmount()
    })
  })
})
