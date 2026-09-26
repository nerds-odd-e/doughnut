import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { notePropertyHref, noteShowHref } from "@/routes/noteShowLocation"
import {
  mountAndPaste,
  mountNoteEditableContent,
  setupPopupsMock,
  setupUpdateNoteContentMock,
  useOriginalText,
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
    const { wrapper, textarea } = await mountAndPaste(
      "existing text",
      "<p><strong>Bold text</strong></p>",
      { selection: [8, 8] }
    )

    expect(textarea.value).toContain("Bold text")
    expect(textarea.value).toContain("existing")
    expect(mockPopupsOptions).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  describe("link removal prompt", () => {
    it("shows options popup when pasted content contains links and removes them when chosen", async () => {
      mockPopupsOptions.mockResolvedValue("links")

      const { wrapper, textarea } = await mountAndPaste(
        "[existing link](https://existing.com) ",
        '<p><a href="https://example.com">new link</a></p>'
      )

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
      const { wrapper, textarea } = await mountAndPaste(
        "See ",
        `<p><a href="${relative}">rel</a> <a href="${absolute}">abs</a> <a href="${property}">prop</a></p>`,
        { selection: [4, 4] }
      )

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

    it("shows options popup based on content after rich editor paste", async () => {
      const wrapper = await mountNoteEditableContent(
        { noteId: 1, noteContent: "plain text", asMarkdown: false },
        { attachTo: document.body }
      )

      const newContent = "plain text [new link](https://example.com)"
      const richEditor = wrapper.findComponent({ name: "RichMarkdownEditor" })
      richEditor.vm.$emit("update:modelValue", newContent)
      richEditor.vm.$emit("pasteComplete", newContent)
      await flushPromises()

      expect(mockPopupsOptions).toHaveBeenCalledWith(
        "The content contains 1 links.",
        expect.arrayContaining([{ label: "Remove 1 links", value: "links" }])
      )
      wrapper.unmount()
    })
  })

  // The same paste can trigger both the pending paste choice and the
  // whole-note link removal prompt; these cover only their coexistence.
  describe("alongside a paste choice", () => {
    const linkHtml = '<p><a href="https://example.com">new link</a></p>'
    const rawOriginal = "RAW original text"

    async function mountAndPasteLinkHtml() {
      const pasted = await mountAndPaste("", linkHtml, {
        plainText: rawOriginal,
      })
      expect(mockPopupsOptions).toHaveBeenCalled()
      return pasted
    }

    it("resumes the paste choice after the removal prompt is cancelled", async () => {
      const { wrapper, textarea } = await mountAndPasteLinkHtml()

      await useOriginalText(wrapper)

      expect(textarea.value).toBe(rawOriginal)
      wrapper.unmount()
    })

    it("consumes the paste choice when the removal prompt's removal is applied", async () => {
      mockPopupsOptions.mockResolvedValue("links")

      const { wrapper, textarea } = await mountAndPasteLinkHtml()

      expect(wrapper.find('[data-testid="paste-choice"]').exists()).toBe(false)
      expect(textarea.value).toContain("new link")
      expect(textarea.value).not.toContain("https://example.com")
      expect(textarea.value).not.toContain(rawOriginal)
      wrapper.unmount()
    })

    it("does not reopen the removal prompt when the alternative is applied", async () => {
      const { wrapper } = await mountAndPaste("", "<p>Styled text</p>", {
        plainText: "**RAW markdown**",
      })
      expect(mockPopupsOptions).not.toHaveBeenCalled()

      await useOriginalText(wrapper)

      expect(mockPopupsOptions).not.toHaveBeenCalled()
      wrapper.unmount()
    })
  })
})
