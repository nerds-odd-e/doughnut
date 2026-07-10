import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"
import {
  addPropertyTapCases,
  deadLinkPropertyMarkdown,
  existingPropertyValueMarkdown,
  expectElementFocused,
  mountTouchFocusEditor,
  PROPERTY_KEY_INPUT,
  PROPERTY_VALUE_INPUT,
} from "./propertyTouchFocusTestSupport"

describe("RichMarkdownEditor property touch focus", () => {
  const h = createRichMarkdownEditorTestHarness()
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    h.cleanup()
  })

  it.each(
    addPropertyTapCases
  )("focuses primer synchronously when Add property is tapped with $case on touch device", async ({
    markdown,
  }) => {
    const { matchMediaSpy: spy, primer } = await mountTouchFocusEditor(
      h,
      markdown,
      true
    )
    matchMediaSpy = spy
    expect(primer).toBeTruthy()

    h.tapAddProperty()

    expect(document.activeElement).toBe(primer)
  })

  it.each(
    addPropertyTapCases
  )("transfers focus to property key after insert form mounts with $case", async ({
    markdown,
  }) => {
    const { matchMediaSpy: spy } = await mountTouchFocusEditor(
      h,
      markdown,
      true
    )
    matchMediaSpy = spy

    await h.openAddProperty()
    await h.flushAnimationFrame()
    expectElementFocused(PROPERTY_KEY_INPUT)
  })

  it("does not focus primer when pointer is not coarse", async () => {
    const { matchMediaSpy: spy, primer } = await mountTouchFocusEditor(
      h,
      "# Hello Body",
      false
    )
    matchMediaSpy = spy

    await h.openAddProperty()
    await h.flushAnimationFrame()
    expect(document.activeElement).not.toBe(primer)
    expectElementFocused(PROPERTY_KEY_INPUT)
  })

  describe("existing property value", () => {
    it("focuses primer synchronously when value is pointerdown-tapped on touch device", async () => {
      const { matchMediaSpy: spy, primer } = await mountTouchFocusEditor(
        h,
        existingPropertyValueMarkdown,
        true
      )
      matchMediaSpy = spy
      expect(primer).toBeTruthy()

      h.pointerdownPropertyValueField()

      expect(document.activeElement).toBe(primer)
    })

    it("transfers focus to value field after pointerdown on touch device", async () => {
      const { matchMediaSpy: spy } = await mountTouchFocusEditor(
        h,
        existingPropertyValueMarkdown,
        true
      )
      matchMediaSpy = spy

      h.pointerdownPropertyValueField()
      h.completePropertyValueFieldTap()
      expectElementFocused(PROPERTY_VALUE_INPUT)
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

    it("does not focus primer when pointerdown hits a dead wiki link", async () => {
      const { matchMediaSpy: spy, primer } = await mountTouchFocusEditor(
        h,
        deadLinkPropertyMarkdown,
        true
      )
      matchMediaSpy = spy
      const deadLink = h
        .propertyValueFieldElement()
        .querySelector("a.dead-link")
      expect(deadLink).toBeTruthy()

      deadLink!.dispatchEvent(
        new PointerEvent("pointerdown", { bubbles: true })
      )

      expect(document.activeElement).not.toBe(primer)
    })
  })
})
