import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import {
  choiceShown,
  mountAndPaste,
  setupPopupsMock,
  setupUpdateNoteContentMock,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

const TOUCH_TARGET_MIN_PX = 44
const EXPIRY_MS = 10_000
const originalMarkdown = "**RAW markdown**"

type Wrapper = VueWrapper<ComponentPublicInstance>

function el(wrapper: Wrapper, testId: string) {
  return wrapper.find(`[data-testid="${testId}"]`).element as HTMLElement
}

describe("NoteEditableContent paste choice action bar", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    setupUpdateNoteContentMock()
    setupPopupsMock(vi.fn().mockResolvedValue(null))
  })

  afterEach(() => {
    document.body.innerHTML = ""
  })

  // Pastes over "before", so applying the choice yields `${originalMarkdown} after`.
  function mountWithPasteChoice() {
    return mountAndPaste("before after", "<p>Styled text</p>", {
      plainText: originalMarkdown,
      selection: [0, "before".length],
    })
  }

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

      expect(choiceShown(wrapper)).toBe(false)
      expect(textarea.value).toBe(contentBeforeDismissal)
      wrapper.unmount()
    }
  )

  describe("expiry", () => {
    beforeEach(() => vi.useFakeTimers())
    afterEach(() => vi.useRealTimers())

    it("clears an ignored choice after the timeout, leaving the pasted content unchanged", async () => {
      const { wrapper, textarea } = await mountWithPasteChoice()
      const contentBeforeExpiry = textarea.value

      await vi.advanceTimersByTimeAsync(EXPIRY_MS)

      expect(choiceShown(wrapper)).toBe(false)
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
      const action = el(wrapper, "paste-choice-action")
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
        expect(choiceShown(wrapper)).toBe(true)

        await setInteraction(interaction, false, wrapper)
        await vi.advanceTimersByTimeAsync(EXPIRY_MS)
        expect(choiceShown(wrapper)).toBe(false)
        wrapper.unmount()
      }
    )

    it("does not throw when unmounted with a pending expiry timer", async () => {
      const { wrapper } = await mountWithPasteChoice()

      expect(() => wrapper.unmount()).not.toThrow()
      await vi.advanceTimersByTimeAsync(EXPIRY_MS)
    })
  })

  describe("reachability", () => {
    it("renders both the action and dismiss buttons at least 44 CSS px in each dimension", async () => {
      const { wrapper } = await mountWithPasteChoice()

      for (const testId of ["paste-choice-action", "paste-choice-dismiss"]) {
        const rect = el(wrapper, testId).getBoundingClientRect()
        expect(rect.width).toBeGreaterThanOrEqual(TOUCH_TARGET_MIN_PX)
        expect(rect.height).toBeGreaterThanOrEqual(TOUCH_TARGET_MIN_PX)
      }
      wrapper.unmount()
    })

    it("gives the action button an accessible name from its own text content", async () => {
      const { wrapper } = await mountWithPasteChoice()

      expect(el(wrapper, "paste-choice-action").textContent?.trim()).toBe(
        "Use original text"
      )
      expect(
        el(wrapper, "paste-choice-dismiss").getAttribute("aria-label")
      ).toBe("Dismiss")
      wrapper.unmount()
    })

    it("activates the action via the keyboard (focus + click while focused) and still replaces correctly", async () => {
      const { wrapper, textarea } = await mountWithPasteChoice()
      const action = el(wrapper, "paste-choice-action")

      action.focus()
      expect(document.activeElement).toBe(action)
      action.dispatchEvent(new MouseEvent("click", { bubbles: true }))
      await flushPromises()

      expect(textarea.value).toBe(`${originalMarkdown} after`)
      wrapper.unmount()
    })

    it("keeps the textarea focused when the action is activated by mousedown+click (pointer activation)", async () => {
      const { wrapper, textarea } = await mountWithPasteChoice()
      const action = el(wrapper, "paste-choice-action")
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

    describe("reduced viewport height", () => {
      const originalInnerHeight = window.innerHeight
      const setInnerHeight = (value: number) =>
        Object.defineProperty(window, "innerHeight", {
          value,
          writable: true,
          configurable: true,
        })

      beforeEach(() => {
        // Room above the textarea for the bar to flip into once below no longer fits.
        document.body.style.paddingTop = "400px"
      })

      afterEach(() => {
        document.body.style.paddingTop = ""
        setInnerHeight(originalInnerHeight)
      })

      it("keeps the action bar within the viewport and clear of the textarea when the natural position would overflow", async () => {
        const { wrapper, textarea } = await mountWithPasteChoice()
        const textareaRect = textarea.getBoundingClientRect()

        setInnerHeight(Math.round(textareaRect.bottom + 10))
        window.dispatchEvent(new Event("resize"))
        await flushPromises()

        const barRect = el(wrapper, "paste-choice").getBoundingClientRect()
        expect(barRect.bottom).toBeLessThanOrEqual(window.innerHeight)
        expect(barRect.bottom).toBeLessThanOrEqual(textareaRect.top)
        wrapper.unmount()
      })
    })
  })
})
