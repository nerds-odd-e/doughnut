import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import {
  createClipboardEvent,
  dispatchRichPaste,
  clearNativeSelectionForQuillMutation,
  mountNoteEditableContent,
  richQuillInstance,
  setupPopupsMock,
  setupUpdateNoteContentMock,
  textareaEl,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

const TOUCH_TARGET_MIN_PX = 44

describe("NoteEditableContent paste choice viewport reachability", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    setupUpdateNoteContentMock()
    setupPopupsMock(vi.fn().mockResolvedValue(null))
  })

  afterEach(() => {
    document.body.innerHTML = ""
  })

  const convertedHtml = "<p>Styled text</p>"
  const originalMarkdown = "**RAW markdown**"

  async function mountMarkdownWithPasteChoice() {
    const wrapper = mountNoteEditableContent(
      { noteId: 1, noteContent: "before after" },
      { attachTo: document.body }
    )
    await flushPromises()
    const textarea = textareaEl(wrapper)
    textarea.setSelectionRange(0, "before".length)
    await textarea.dispatchEvent(
      createClipboardEvent(convertedHtml, originalMarkdown)
    )
    await flushPromises()
    return { wrapper, textarea }
  }

  function pasteChoiceButtons(
    wrapper: Awaited<ReturnType<typeof mountMarkdownWithPasteChoice>>["wrapper"]
  ) {
    return {
      action: wrapper.find('[data-testid="paste-choice-action"]')
        .element as HTMLElement,
      dismiss: wrapper.find('[data-testid="paste-choice-dismiss"]')
        .element as HTMLElement,
    }
  }

  it("renders both the action and dismiss buttons at least 44 CSS px in each dimension", async () => {
    const { wrapper } = await mountMarkdownWithPasteChoice()
    const { action, dismiss } = pasteChoiceButtons(wrapper)

    for (const button of [action, dismiss]) {
      const rect = button.getBoundingClientRect()
      expect(rect.width).toBeGreaterThanOrEqual(TOUCH_TARGET_MIN_PX)
      expect(rect.height).toBeGreaterThanOrEqual(TOUCH_TARGET_MIN_PX)
    }
    wrapper.unmount()
  })

  it("gives the action button an accessible name from its own text content", async () => {
    const { wrapper } = await mountMarkdownWithPasteChoice()
    const { action, dismiss } = pasteChoiceButtons(wrapper)

    expect(action.textContent?.trim()).toBe("Use original text")
    expect(dismiss.getAttribute("aria-label")).toBe("Dismiss")
    wrapper.unmount()
  })

  it("activates the action via the keyboard (focus + click while focused) and still replaces correctly", async () => {
    const { wrapper, textarea } = await mountMarkdownWithPasteChoice()
    const { action } = pasteChoiceButtons(wrapper)

    action.focus()
    expect(document.activeElement).toBe(action)
    action.dispatchEvent(new MouseEvent("click", { bubbles: true }))
    await flushPromises()

    expect(textarea.value).toBe(`${originalMarkdown} after`)
    wrapper.unmount()
  })

  it("keeps the textarea focused when the action is activated by mousedown+click (pointer activation)", async () => {
    const { wrapper, textarea } = await mountMarkdownWithPasteChoice()
    const { action } = pasteChoiceButtons(wrapper)
    textarea.focus()
    expect(document.activeElement).toBe(textarea)

    const mousedown = new MouseEvent("mousedown", {
      bubbles: true,
      cancelable: true,
    })
    action.dispatchEvent(mousedown)
    expect(mousedown.defaultPrevented).toBe(true)
    action.dispatchEvent(new MouseEvent("click", { bubbles: true }))
    await flushPromises()

    expect(textarea.value).toBe(`${originalMarkdown} after`)
    wrapper.unmount()
  })

  describe("markdown mode, reduced viewport height", () => {
    let originalInnerHeight: number

    beforeEach(() => {
      originalInnerHeight = window.innerHeight
      // Room above the textarea for the bar to flip into once below no longer fits.
      document.body.style.paddingTop = "400px"
    })

    afterEach(() => {
      document.body.style.paddingTop = ""
      Object.defineProperty(window, "innerHeight", {
        value: originalInnerHeight,
        writable: true,
        configurable: true,
      })
    })

    it("keeps the action bar within the viewport and clear of the textarea when the natural position would overflow", async () => {
      const { wrapper, textarea } = await mountMarkdownWithPasteChoice()
      const textareaRect = textarea.getBoundingClientRect()

      Object.defineProperty(window, "innerHeight", {
        value: Math.round(textareaRect.bottom + 10),
        writable: true,
        configurable: true,
      })
      window.dispatchEvent(new Event("resize"))
      await flushPromises()

      const bar = wrapper.find('[data-testid="paste-choice"]')
        .element as HTMLElement
      const barRect = bar.getBoundingClientRect()

      expect(barRect.bottom).toBeLessThanOrEqual(window.innerHeight)
      expect(barRect.bottom).toBeLessThanOrEqual(textareaRect.top)
      wrapper.unmount()
    })
  })

  describe("rich mode", () => {
    const richOriginalText = "RAWORIGINALTEXT"

    it("positions the action bar clear of the geometry captured for the pasted span", async () => {
      const wrapper = mountNoteEditableContent(
        { noteId: 1, noteContent: "Hello world today", asMarkdown: false },
        { attachTo: document.body }
      )
      await flushPromises()
      // Quill's own `getBounds()` is what `pasteInsertionViewportRect` (and so the
      // captured `PasteChoice.anchorRect`) is built from; spying on it observes the
      // exact geometry the production code anchored the bar to - not a DOM query
      // repeated later, whose result can differ if unrelated sibling UI (e.g. the
      // frontmatter property controls above the editor) settles into a different
      // height between paste and assertion, independent of this positioning code.
      const quill = richQuillInstance(wrapper)
      const getBoundsSpy = vi.spyOn(quill, "getBounds")

      await dispatchRichPaste(wrapper, "<p>Lost</p>", {
        plainText: richOriginalText,
        selection: { index: 6, length: 5 },
      })
      expect(wrapper.find('[data-testid="paste-choice-action"]').exists()).toBe(
        true
      )

      const anchor = getBoundsSpy.mock.results[0]?.value as {
        top: number
        bottom: number
      }
      expect(anchor).toBeTruthy()

      const bar = wrapper.find('[data-testid="paste-choice"]')
        .element as HTMLElement
      const barRect = bar.getBoundingClientRect()

      const overlapsVertically =
        barRect.top < anchor.bottom && barRect.bottom > anchor.top
      expect(overlapsVertically).toBe(false)

      clearNativeSelectionForQuillMutation()
      wrapper.unmount()
    })
  })
})
