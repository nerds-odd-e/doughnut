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

describe("NoteEditableContent paste choice", () => {
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

  async function mountWithPasteChoice(noteContent = "") {
    const wrapper = mountNoteEditableContent(
      { noteId: 1, noteContent },
      { attachTo: document.body }
    )
    await flushPromises()
    const textarea = textareaEl(wrapper)
    await textarea.dispatchEvent(
      createClipboardEvent(convertedHtml, originalMarkdown)
    )
    await flushPromises()
    expect(wrapper.find('[data-testid="paste-choice-action"]').exists()).toBe(
      true
    )
    return { wrapper, textarea }
  }

  type Wrapper = Awaited<ReturnType<typeof mountWithPasteChoice>>["wrapper"]

  it("replaces a selected paste with the original markdown, keeping surrounding text", async () => {
    const wrapper = mountNoteEditableContent(
      { noteId: 1, noteContent: "before [SELECTED] after" },
      { attachTo: document.body }
    )
    await flushPromises()
    const textarea = textareaEl(wrapper)
    textarea.setSelectionRange("before ".length, "before [SELECTED]".length)

    await textarea.dispatchEvent(
      createClipboardEvent(convertedHtml, originalMarkdown)
    )
    await flushPromises()

    await wrapper.find('[data-testid="paste-choice-action"]').trigger("click")
    await flushPromises()

    expect(textarea.value).toBe(`before ${originalMarkdown} after`)
    wrapper.unmount()
  })

  it("replaces a multiline paste with the original markdown", async () => {
    const wrapper = mountNoteEditableContent(
      { noteId: 1, noteContent: "start end" },
      { attachTo: document.body }
    )
    await flushPromises()
    const textarea = textareaEl(wrapper)
    textarea.setSelectionRange(5, 5)
    const multilineOriginal = "line one\nline two"

    await textarea.dispatchEvent(
      createClipboardEvent("<p>x</p><p>y</p>", multilineOriginal)
    )
    await flushPromises()

    await wrapper.find('[data-testid="paste-choice-action"]').trigger("click")
    await flushPromises()

    expect(textarea.value).toBe(`start${multilineOriginal} end`)
    wrapper.unmount()
  })

  it("keeps the converted text and hides the action after dismissing the choice", async () => {
    const { wrapper, textarea } = await mountWithPasteChoice()
    const convertedValue = textarea.value
    expect(convertedValue).not.toContain(originalMarkdown)

    await wrapper.find('[data-testid="paste-choice-dismiss"]').trigger("click")
    await flushPromises()

    expect(textarea.value).toBe(convertedValue)
    expect(wrapper.find('[data-testid="paste-choice"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it("offers no recovery action for HTML-only paste fixtures", async () => {
    const wrapper = mountNoteEditableContent(
      { noteId: 1, noteContent: "" },
      { attachTo: document.body }
    )
    await flushPromises()
    const textarea = textareaEl(wrapper)

    await textarea.dispatchEvent(
      createClipboardEvent("<p><strong>Bold text</strong></p>")
    )
    await flushPromises()

    expect(textarea.value).toContain("Bold text")
    expect(wrapper.find('[data-testid="paste-choice"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it("replaces the pending choice when a new paste immediately follows", async () => {
    const { wrapper, textarea } = await mountWithPasteChoice()
    const secondOriginal = "SECOND_ORIGINAL_MARKDOWN"

    await textarea.dispatchEvent(
      createClipboardEvent("<p>Second</p>", secondOriginal)
    )
    await flushPromises()

    await wrapper.find('[data-testid="paste-choice-action"]').trigger("click")
    await flushPromises()

    expect(textarea.value).toContain(secondOriginal)
    expect(textarea.value).not.toContain(originalMarkdown)
    wrapper.unmount()
  })

  it("invalidates the choice when the user types after pasting", async () => {
    const { wrapper, textarea } = await mountWithPasteChoice()

    textarea.value += "x"
    textarea.dispatchEvent(new Event("input"))
    await flushPromises()

    expect(wrapper.find('[data-testid="paste-choice"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it("invalidates the choice when switching to a different note", async () => {
    const { wrapper } = await mountWithPasteChoice()

    await wrapper.setProps({ noteId: 2, noteContent: "different note" })
    await flushPromises()

    expect(wrapper.find('[data-testid="paste-choice"]').exists()).toBe(false)
    wrapper.unmount()
  })

  type Dismissal = "escape" | "outsideClick" | "explicitDismissal"

  async function dismiss(dismissal: Dismissal, wrapper: Wrapper) {
    if (dismissal === "escape") {
      document.dispatchEvent(
        new KeyboardEvent("keydown", { key: "Escape", bubbles: true })
      )
      return
    }
    if (dismissal === "outsideClick") {
      document.body.dispatchEvent(
        new MouseEvent("mousedown", { bubbles: true })
      )
      return
    }
    await wrapper.find('[data-testid="paste-choice-dismiss"]').trigger("click")
  }

  it.each<Dismissal>(["escape", "outsideClick", "explicitDismissal"])(
    "clears the choice without changing content on %s",
    async (dismissal) => {
      const { wrapper, textarea } = await mountWithPasteChoice()
      const contentBeforeDismissal = textarea.value

      await dismiss(dismissal, wrapper)
      await flushPromises()

      expect(wrapper.find('[data-testid="paste-choice"]').exists()).toBe(false)
      expect(textarea.value).toBe(contentBeforeDismissal)
      wrapper.unmount()
    }
  )

  describe("expiry", () => {
    const EXPIRY_MS = 10_000

    beforeEach(() => vi.useFakeTimers())
    afterEach(() => vi.useRealTimers())

    it("clears an ignored choice after the timeout, leaving the pasted content unchanged", async () => {
      const { wrapper, textarea } = await mountWithPasteChoice()
      const contentBeforeExpiry = textarea.value

      await vi.advanceTimersByTimeAsync(EXPIRY_MS)

      expect(wrapper.find('[data-testid="paste-choice"]').exists()).toBe(false)
      expect(textarea.value).toBe(contentBeforeExpiry)
      wrapper.unmount()
    })

    type Interaction = "hover" | "focus"

    async function setInteraction(
      interaction: Interaction,
      active: boolean,
      wrapper: Wrapper
    ) {
      if (interaction === "hover") {
        const event = active ? "mouseenter" : "mouseleave"
        await wrapper.find('[data-testid="paste-choice"]').trigger(event)
        return
      }
      const action = wrapper.find('[data-testid="paste-choice-action"]')
        .element as HTMLElement
      if (active) action.focus()
      else action.blur()
      await flushPromises()
    }

    it.each<Interaction>(["hover", "focus"])(
      "pauses expiry during %s and resumes once it ends",
      async (interaction) => {
        const { wrapper } = await mountWithPasteChoice()

        await setInteraction(interaction, true, wrapper)
        await vi.advanceTimersByTimeAsync(EXPIRY_MS)
        expect(wrapper.find('[data-testid="paste-choice"]').exists()).toBe(true)

        await setInteraction(interaction, false, wrapper)
        await vi.advanceTimersByTimeAsync(EXPIRY_MS)
        expect(wrapper.find('[data-testid="paste-choice"]').exists()).toBe(
          false
        )
        wrapper.unmount()
      }
    )

    it("does not throw when unmounted with a pending expiry timer", async () => {
      const { wrapper } = await mountWithPasteChoice()

      expect(() => wrapper.unmount()).not.toThrow()
      await vi.advanceTimersByTimeAsync(EXPIRY_MS)
    })
  })
})
