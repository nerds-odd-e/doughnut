import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import {
  choiceShown,
  clearNativeSelectionForQuillMutation,
  dispatchRichPaste,
  mountAndPaste,
  mountNoteEditableContent,
  pasteIntoTextarea,
  richQuillEditorEl,
  richQuillInstance,
  setupPopupsMock,
  setupUpdateNoteContentMock,
  textareaEl,
  useOriginalText,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

const convertedHtml = "<p>Styled text</p>"
const originalMarkdown = "**RAW markdown**"

describe("NoteEditableContent paste choice", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    setupUpdateNoteContentMock()
    setupPopupsMock(vi.fn().mockResolvedValue(null))
  })

  afterEach(() => {
    document.body.innerHTML = ""
  })

  async function mountWithPasteChoice() {
    const pasted = await mountAndPaste("", convertedHtml, {
      plainText: originalMarkdown,
    })
    expect(choiceShown(pasted.wrapper)).toBe(true)
    return pasted
  }

  describe("markdown mode", () => {
    it("replaces a selected paste with the original markdown, keeping surrounding text", async () => {
      const { wrapper, textarea } = await mountAndPaste(
        "before [SELECTED] after",
        convertedHtml,
        {
          plainText: originalMarkdown,
          selection: ["before ".length, "before [SELECTED]".length],
        }
      )

      await useOriginalText(wrapper)

      expect(textarea.value).toBe(`before ${originalMarkdown} after`)
      wrapper.unmount()
    })

    it("replaces a multiline paste with the original markdown", async () => {
      const multilineOriginal = "line one\nline two"
      const { wrapper, textarea } = await mountAndPaste(
        "start end",
        "<p>x</p><p>y</p>",
        { plainText: multilineOriginal, selection: [5, 5] }
      )

      await useOriginalText(wrapper)

      expect(textarea.value).toBe(`start${multilineOriginal} end`)
      wrapper.unmount()
    })

    it("offers no recovery action for HTML-only paste fixtures", async () => {
      const { wrapper, textarea } = await mountAndPaste(
        "",
        "<p><strong>Bold text</strong></p>"
      )

      expect(textarea.value).toContain("Bold text")
      expect(choiceShown(wrapper)).toBe(false)
      wrapper.unmount()
    })

    it("replaces the pending choice when a new paste immediately follows", async () => {
      const { wrapper, textarea } = await mountWithPasteChoice()
      const secondOriginal = "SECOND_ORIGINAL_MARKDOWN"

      await pasteIntoTextarea(textarea, "<p>Second</p>", secondOriginal)
      await useOriginalText(wrapper)

      expect(textarea.value).toContain(secondOriginal)
      expect(textarea.value).not.toContain(originalMarkdown)
      wrapper.unmount()
    })

    it("invalidates the choice when the user types after pasting", async () => {
      const { wrapper, textarea } = await mountWithPasteChoice()

      textarea.value += "x"
      textarea.dispatchEvent(new Event("input"))
      await flushPromises()

      expect(choiceShown(wrapper)).toBe(false)
      wrapper.unmount()
    })

    it("invalidates the choice when switching to a different note", async () => {
      const { wrapper } = await mountWithPasteChoice()

      await wrapper.setProps({ noteId: 2, noteContent: "different note" })
      await flushPromises()

      expect(choiceShown(wrapper)).toBe(false)
      wrapper.unmount()
    })
  })

  describe("rich mode", () => {
    const richOriginalText = "RAWORIGINALTEXT"

    function mountRich(noteContent: string) {
      return mountNoteEditableContent(
        { noteId: 1, noteContent, asMarkdown: false },
        { attachTo: document.body }
      )
    }

    // "Hello world today": replace the word "world" (index 6, length 5) with
    // content that loses the original clipboard text, so a choice is offered.
    async function pasteLosingOriginal(
      wrapper: VueWrapper<ComponentPublicInstance>
    ) {
      await dispatchRichPaste(wrapper, "<p>Lost</p>", {
        plainText: richOriginalText,
        selection: { index: 6, length: 5 },
      })
      expect(choiceShown(wrapper)).toBe(true)
    }

    async function mountRichWithPasteChoice() {
      const wrapper = await mountRich(
        "---\ntopic: training\n---\n\nHello world today"
      )
      await pasteLosingOriginal(wrapper)
      clearNativeSelectionForQuillMutation()
      return wrapper
    }

    it("replaces the just-pasted content with the original text, preserving surrounding content and frontmatter, also as Markdown", async () => {
      const wrapper = await mountRichWithPasteChoice()

      await useOriginalText(wrapper)

      expect(richQuillEditorEl(wrapper).textContent).toBe(
        `Hello ${richOriginalText} today`
      )
      const composed = wrapper
        .findComponent({ name: "RichMarkdownEditor" })
        .props("modelValue") as string

      await wrapper.setProps({ asMarkdown: true })
      await flushPromises()

      for (const content of [composed, textareaEl(wrapper).value]) {
        expect(content).toContain(richOriginalText)
        expect(content).toContain("Hello")
        expect(content).toContain("today")
        expect(content).toContain("topic: training")
      }
      wrapper.unmount()
    })

    it("invalidates the choice when the user types in the rich editor after pasting", async () => {
      const wrapper = await mountRichWithPasteChoice()

      richQuillInstance(wrapper).insertText(0, "x", "user")
      await flushPromises()

      expect(choiceShown(wrapper)).toBe(false)
      wrapper.unmount()
    })

    it("positions the action bar clear of the geometry captured for the pasted span", async () => {
      const wrapper = await mountRich("Hello world today")
      // Quill's own `getBounds()` is what `pasteInsertionViewportRect` (and so the
      // captured `PasteChoice.anchorRect`) is built from; spying on it observes the
      // exact geometry the production code anchored the bar to - not a DOM query
      // repeated later, whose result can differ if unrelated sibling UI (e.g. the
      // frontmatter property controls above the editor) settles into a different
      // height between paste and assertion, independent of this positioning code.
      const getBoundsSpy = vi.spyOn(richQuillInstance(wrapper), "getBounds")

      await pasteLosingOriginal(wrapper)

      const anchor = getBoundsSpy.mock.results[0]?.value as {
        top: number
        bottom: number
      }
      expect(anchor).toBeTruthy()
      const barRect = wrapper
        .find('[data-testid="paste-choice"]')
        .element.getBoundingClientRect()
      const overlapsVertically =
        barRect.top < anchor.bottom && barRect.bottom > anchor.top
      expect(overlapsVertically).toBe(false)

      clearNativeSelectionForQuillMutation()
      wrapper.unmount()
    })
  })
})
