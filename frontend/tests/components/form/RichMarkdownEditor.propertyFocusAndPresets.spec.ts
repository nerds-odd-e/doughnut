import { advanceAnimationFrame } from "@tests/helpers/focusTargetTestSupport"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const INSERT_KEY_INPUT =
  '[data-property-draft="true"] [data-testid="rich-note-property-row-key-input"]'
const ROW_VALUE_INPUT = '[data-testid="rich-note-property-row-value-input"]'

function inputEl(selector: string): HTMLInputElement {
  const el = document.querySelector(selector) as HTMLInputElement | null
  expect(el).not.toBeNull()
  return el!
}

function expectElementFocused(selector: string) {
  expect(document.activeElement).toBe(inputEl(selector))
}

describe("RichMarkdownEditor property focus", () => {
  const h = createRichMarkdownEditorTestHarness()

  async function mountTouchFocusEditor(markdown: string, coarse: boolean) {
    mockCoarsePointer(coarse)
    mountSoftKeyboardPrimer()
    await h.mountEditor(markdown, { attachToBody: true })
    return softKeyboardPrimerElement()
  }

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
    vi.useRealTimers()
  })

  it("Enter in the draft key focuses its value without adding", async () => {
    const wrapper = await h.mountEditor("# Body", { attachToBody: true })
    await h.openAddProperty()
    await wrapper.find(INSERT_KEY_INPUT).setValue("topic")
    await wrapper.find(INSERT_KEY_INPUT).trigger("keydown", { key: "Enter" })
    await advanceAnimationFrame()
    expectElementFocused(`[data-property-draft="true"] ${ROW_VALUE_INPUT}`)
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()
  })

  describe("touch focus", () => {
    it.each([
      { case: "no existing rows", markdown: "# Hello Body" },
      { case: "existing rows", markdown: "---\nstatus: ok\n---\n\n# Body" },
    ])(
      "Add property on touch focuses primer then property key with $case",
      async ({ markdown }) => {
        const primer = await mountTouchFocusEditor(markdown, true)
        expect(primer).toBeTruthy()

        h.tapAddProperty()
        expect(document.activeElement).toBe(primer)

        await flushPromises()
        await advanceAnimationFrame()
        expectElementFocused(INSERT_KEY_INPUT)
      }
    )

    it("does not focus primer on Add property when pointer is not coarse", async () => {
      const primer = await mountTouchFocusEditor("# Hello Body", false)

      await h.openAddProperty()
      await advanceAnimationFrame()
      expect(document.activeElement).not.toBe(primer)
      expectElementFocused(INSERT_KEY_INPUT)
    })

    it("focuses primer then existing value field on touch; skips primer for dead wiki link", async () => {
      const primer = await mountTouchFocusEditor(
        `---
plain: training
wiki: "[[Missing Note]]"
---

Workshop body.`,
        true
      )
      expect(primer).toBeTruthy()

      h.pointerdownPropertyValueField()
      expect(document.activeElement).toBe(primer)

      h.completePropertyValueFieldTap()
      expectElementFocused(ROW_VALUE_INPUT)

      const deadLink = h
        .getWrapper()
        .element.querySelector(`${ROW_VALUE_INPUT} a.dead-wiki-link`)
      expect(deadLink).toBeTruthy()
      deadLink!.dispatchEvent(
        new PointerEvent("pointerdown", { bubbles: true })
      )
      expect(document.activeElement).not.toBe(primer)
    })

    it("does not focus primer on an existing value field when pointer is not coarse", async () => {
      const primer = await mountTouchFocusEditor(
        "---\ntopic: training\n---\n\nWorkshop body.",
        false
      )

      h.pointerdownPropertyValueField()
      h.completePropertyValueFieldTap()

      expect(document.activeElement).not.toBe(primer)
      expectElementFocused(ROW_VALUE_INPUT)
    })
  })
})
