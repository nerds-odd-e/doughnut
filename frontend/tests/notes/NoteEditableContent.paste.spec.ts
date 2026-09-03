import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { notePropertyHref, noteShowHref } from "@/routes/noteShowLocation"
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

  it("converts HTML to markdown when pasting HTML content without links", async () => {
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
    expect(mockPopupsOptions).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  describe("textarea", () => {
    it("shows options popup when pasted content contains links and removes them when chosen", async () => {
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

      expect(mockPopupsOptions).toHaveBeenCalledWith(
        "The content contains 2 links.",
        expect.arrayContaining([{ label: "Remove 2 links", value: "links" }])
      )
      expect(textarea.value).toContain("existing link")
      expect(textarea.value).toContain("new link")
      expect(textarea.value).not.toContain("https://existing.com")
      expect(textarea.value).not.toContain("https://example.com")
      wrapper.unmount()
    })

    it("preserves relative and absolute note URLs as markdown links on paste", async () => {
      const relative = noteShowHref(99)
      const absolute = "https://doughnut.odd-e.com/n42"
      const property = notePropertyHref(7, "topic")
      const wrapper = mountNoteEditableContent(
        { noteId: 1, noteContent: "See " },
        { attachTo: document.body }
      )
      await flushPromises()

      const textarea = textareaEl(wrapper)
      textarea.setSelectionRange(4, 4)
      await textarea.dispatchEvent(
        createClipboardEvent(
          `<p><a href="${relative}">rel</a> <a href="${absolute}">abs</a> <a href="${property}">prop</a></p>`
        )
      )
      await flushPromises()

      expect(textarea.value).toContain(`[rel](${relative})`)
      expect(textarea.value).toContain(`[abs](${absolute})`)
      expect(textarea.value).toContain(`[prop](${property})`)
      expect(textarea.value).not.toContain("[[")
      expect(mockPopupsOptions).toHaveBeenCalledWith(
        "The content contains 3 links.",
        expect.arrayContaining([{ label: "Remove 3 links", value: "links" }])
      )
      wrapper.unmount()
    })
  })

  it("shows options popup based on content after rich editor paste", async () => {
    const wrapper = mountNoteEditableContent(
      { noteId: 1, noteContent: "plain text", asMarkdown: false },
      { attachTo: document.body }
    )
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
})
