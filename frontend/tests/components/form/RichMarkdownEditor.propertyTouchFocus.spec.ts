import { afterEach, beforeEach, vi } from "vitest"
import { flushPromises } from "@vue/test-utils"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"
import { advanceAnimationFrame } from "./propertyKeyPresetsTestDom"
import {
  addPropertyTapCases,
  deadWikiLinkPropertyMarkdown,
  existingPropertyValueMarkdown,
  expectElementFocused,
  mountTouchFocusEditor,
  PROPERTY_KEY_INPUT,
  PROPERTY_VALUE_INPUT,
} from "./propertyTouchFocusTestSupport"

describe("RichMarkdownEditor property touch focus", () => {
  const h = createRichMarkdownEditorTestHarness()
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
  })

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    h.cleanup()
    vi.useRealTimers()
  })

  it.each(addPropertyTapCases)(
    "Add property on touch focuses primer then property key with $case",
    async ({ markdown }) => {
      const { matchMediaSpy: spy, primer } = await mountTouchFocusEditor(
        h,
        markdown,
        true
      )
      matchMediaSpy = spy
      expect(primer).toBeTruthy()

      h.tapAddProperty()
      expect(document.activeElement).toBe(primer)

      await flushPromises()
      await advanceAnimationFrame()
      expectElementFocused(PROPERTY_KEY_INPUT)
    }
  )

  it("does not focus primer when pointer is not coarse", async () => {
    const { matchMediaSpy: spy, primer } = await mountTouchFocusEditor(
      h,
      "# Hello Body",
      false
    )
    matchMediaSpy = spy

    await h.openAddProperty()
    await advanceAnimationFrame()
    expect(document.activeElement).not.toBe(primer)
    expectElementFocused(PROPERTY_KEY_INPUT)
  })

  describe("existing property value", () => {
    it("focuses primer then value field on touch; skips primer for dead wiki link", async () => {
      const { matchMediaSpy: spy, primer } = await mountTouchFocusEditor(
        h,
        existingPropertyValueMarkdown,
        true
      )
      matchMediaSpy = spy
      expect(primer).toBeTruthy()

      h.pointerdownPropertyValueField()
      expect(document.activeElement).toBe(primer)

      h.completePropertyValueFieldTap()
      expectElementFocused(PROPERTY_VALUE_INPUT)

      await h.getWrapper().setProps({
        modelValue: deadWikiLinkPropertyMarkdown,
      })
      await flushPromises()

      const deadLink = h
        .propertyValueFieldElement()
        .querySelector("a.dead-wiki-link")
      expect(deadLink).toBeTruthy()
      deadLink!.dispatchEvent(
        new PointerEvent("pointerdown", { bubbles: true })
      )
      expect(document.activeElement).not.toBe(primer)
    })

    it("does not focus primer when pointer is not coarse", async () => {
      const { matchMediaSpy: spy, primer } = await mountTouchFocusEditor(
        h,
        existingPropertyValueMarkdown,
        false
      )
      matchMediaSpy = spy

      h.pointerdownPropertyValueField()
      h.completePropertyValueFieldTap()

      expect(document.activeElement).not.toBe(primer)
      expectElementFocused(PROPERTY_VALUE_INPUT)
    })
  })
})
