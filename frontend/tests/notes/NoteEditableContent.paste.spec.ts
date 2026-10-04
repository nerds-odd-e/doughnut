import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { notePropertyHref, noteShowHref } from "@/routes/noteShowLocation"
import {
  answerPopup,
  onlyPendingPopup,
  pendingPopups,
} from "@tests/helpers/popupStackTestSupport"
import {
  mountAndPaste,
  mountNoteEditableContent,
  setupUpdateNoteContentMock,
  useOriginalText,
} from "./noteEditableContentTestSupport"

describe("NoteEditableContent paste", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    setupUpdateNoteContentMock()
  })

  function expectLinkRemovalPrompt(linkCount: number) {
    expect(onlyPendingPopup()).toMatchObject({
      type: "options",
      message: `The content contains ${linkCount} links.`,
      options: expect.arrayContaining([
        { label: `Remove ${linkCount} links`, value: "links" },
      ]),
    })
  }

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
    expect(pendingPopups()).toHaveLength(0)
    wrapper.unmount()
  })

  it("leaves the caret after the pasted markdown in a focused textarea", async () => {
    const { wrapper, textarea } = await mountAndPaste(
      "before [SELECTED] after",
      "<p><b>Styled</b> text</p>",
      { selection: [7, 17] }
    )

    expect(textarea.value).toBe("before **Styled** text after")
    expect([textarea.selectionStart, textarea.selectionEnd]).toEqual([22, 22])
    wrapper.unmount()
  })

  it("leaves the caret after the original text when the paste choice replaces it", async () => {
    const { wrapper, textarea } = await mountAndPaste(
      "before [SELECTED] after",
      "<p>Styled text</p>",
      { plainText: "**RAW**", selection: [7, 17] }
    )

    await useOriginalText(wrapper)

    expect(textarea.value).toBe("before **RAW** after")
    expect([textarea.selectionStart, textarea.selectionEnd]).toEqual([14, 14])
    wrapper.unmount()
  })

  describe("link removal prompt", () => {
    it("shows options popup when pasted content contains links and removes them when chosen", async () => {
      const { wrapper, textarea } = await mountAndPaste(
        "[existing link](https://existing.com) ",
        '<p><a href="https://example.com">new link</a></p>'
      )

      expectLinkRemovalPrompt(2)
      await answerPopup("links")

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
      expectLinkRemovalPrompt(3)
      await answerPopup(null)

      expect(textarea.value).toContain(`[rel](${relative})`)
      expect(textarea.value).toContain(`[abs](${absolute})`)
      expect(textarea.value).toContain(`[prop](${property})`)
      expect(textarea.value).not.toContain("[[")
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

      expectLinkRemovalPrompt(1)
      wrapper.unmount()
    })
  })

  // The same paste can trigger both the pending paste choice and the
  // whole-note link removal prompt; these cover only their coexistence.
  describe("alongside a paste choice", () => {
    const linkHtml = '<p><a href="https://example.com">new link</a></p>'
    const rawOriginal = "RAW original text"

    async function mountAndPasteLinkHtml(removalAnswer: "links" | null) {
      const pasted = await mountAndPaste("", linkHtml, {
        plainText: rawOriginal,
      })
      expectLinkRemovalPrompt(1)
      await answerPopup(removalAnswer)
      return pasted
    }

    it("resumes the paste choice after the removal prompt is cancelled", async () => {
      const { wrapper, textarea } = await mountAndPasteLinkHtml(null)

      await useOriginalText(wrapper)

      expect(textarea.value).toBe(rawOriginal)
      wrapper.unmount()
    })

    it("consumes the paste choice when the removal prompt's removal is applied", async () => {
      const { wrapper, textarea } = await mountAndPasteLinkHtml("links")

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
      expect(pendingPopups()).toHaveLength(0)

      await useOriginalText(wrapper)

      expect(pendingPopups()).toHaveLength(0)
      wrapper.unmount()
    })
  })
})
