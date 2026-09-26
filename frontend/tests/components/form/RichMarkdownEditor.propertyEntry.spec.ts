import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { noteShowLocation } from "@/routes/noteShowLocation"
import {
  listPropertyValue,
  parseNoteContentMarkdown,
  richModeKeyDropdownPresetKeysForPropertyRows,
} from "@/utils/noteContentFrontmatter"
import { propertyRowWithScalar } from "@/utils/noteContentPropertyRows"
import {
  advanceAnimationFrame,
  assertPresetOptionsVisible,
  focusKeyInput,
  INSERT_KEY_INPUT,
  keyInputValue,
  ROW_KEY_INPUT,
  selectPresetKey,
} from "./propertyKeyPresetsTestDom"
import {
  addPropertyTapCases,
  existingPropertyValueMarkdown,
  expectElementFocused,
  mountTouchFocusEditor,
  PROPERTY_KEY_INPUT,
  PROPERTY_VALUE_INPUT,
} from "./propertyTouchFocusTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property entry", () => {
  const h = createRichMarkdownEditorTestHarness()

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
    vi.useRealTimers()
  })

  describe("inserting a property", () => {
    it("emits composed frontmatter and preserves body", async () => {
      await h.mountEditor("# Hello Body")
      await h.openAddProperty()
      await advanceAnimationFrame()

      const keyInput = h
        .getWrapper()
        .find('[data-testid="rich-note-property-key"]')
      const valInput = h
        .getWrapper()
        .find('[data-testid="rich-note-property-value"]')
      await keyInput.setValue("status")
      await h.setPropertyValueField(valInput, "draft")
      await valInput.trigger("blur")

      const last = h.lastEmittedMarkdown()
      expect(last).toContain("---")
      expect(last).toContain("status: draft")
      expect(last).toContain("Hello Body")
    })

    it("appends to exact list-capable keys without folding legacy suffixes", async () => {
      await h.mountEditor(
        `---
example of: "[[A]]"
example of 2: "[[B]]"
---

# Body`,
        { attachToBody: true }
      )

      await h.commitInsertProperty("example of", "[[C]]")
      const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
      expect(parsed.ok).toBe(true)
      if (!parsed.ok) return
      expect(parsed.properties["example of"]).toEqual(
        listPropertyValue(["[[A]]", "[[C]]"])
      )
      expect(parsed.properties["example of 2"]).toEqual(
        propertyRowWithScalar("example of 2", "[[B]]").value
      )
    })
  })

  describe("key presets", () => {
    it("offers available presets and sets keys for existing and inserted rows", async () => {
      await h.mountEditor(
        `---
custom: workshop
image: /x.png
---

# Body`,
        { attachToBody: true }
      )
      await focusKeyInput(ROW_KEY_INPUT)
      assertPresetOptionsVisible(
        richModeKeyDropdownPresetKeysForPropertyRows(false, [
          propertyRowWithScalar("custom", "workshop"),
          propertyRowWithScalar("image", "/x.png"),
        ])
      )

      const existingKeyInput = h
        .getWrapper()
        .find(`[data-testid="${ROW_KEY_INPUT}"]`)
      await selectPresetKey("url")
      expect((existingKeyInput.element as HTMLInputElement).value).toBe("url")
      expectElementFocused(
        '[data-property-key="url"] [data-testid="rich-note-property-row-value-input"]'
      )

      await h.openAddProperty()
      await advanceAnimationFrame()
      assertPresetOptionsVisible(
        richModeKeyDropdownPresetKeysForPropertyRows(false, [
          propertyRowWithScalar("image", "/x.png"),
          propertyRowWithScalar("url", "workshop"),
        ])
      )
      await selectPresetKey("wikidata_id")
      expect(keyInputValue(INSERT_KEY_INPUT)).toBe("wikidata_id")
      expectElementFocused(
        '[data-testid="rich-note-wikidata-property-insert-edit"]'
      )
    })
  })

  describe("touch focus", () => {
    it.each(addPropertyTapCases)(
      "Add property on touch focuses primer then property key with $case",
      async ({ markdown }) => {
        const primer = await mountTouchFocusEditor(h, markdown, true)
        expect(primer).toBeTruthy()

        h.tapAddProperty()
        expect(document.activeElement).toBe(primer)

        await flushPromises()
        await advanceAnimationFrame()
        expectElementFocused(PROPERTY_KEY_INPUT)
      }
    )

    it("does not focus primer on Add property when pointer is not coarse", async () => {
      const primer = await mountTouchFocusEditor(h, "# Hello Body", false)

      await h.openAddProperty()
      await advanceAnimationFrame()
      expect(document.activeElement).not.toBe(primer)
      expectElementFocused(PROPERTY_KEY_INPUT)
    })

    it("focuses primer then existing value field on touch; skips primer for dead wiki link", async () => {
      const primer = await mountTouchFocusEditor(
        h,
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
      expectElementFocused(PROPERTY_VALUE_INPUT)

      const deadLink = h
        .getWrapper()
        .element.querySelector(
          '[data-testid="rich-note-property-row-value-input"] a.dead-wiki-link'
        )
      expect(deadLink).toBeTruthy()
      deadLink!.dispatchEvent(
        new PointerEvent("pointerdown", { bubbles: true })
      )
      expect(document.activeElement).not.toBe(primer)
    })

    it("does not focus primer on an existing value field when pointer is not coarse", async () => {
      const primer = await mountTouchFocusEditor(
        h,
        existingPropertyValueMarkdown,
        false
      )

      h.pointerdownPropertyValueField()
      h.completePropertyValueFieldTap()

      expect(document.activeElement).not.toBe(primer)
      expectElementFocused(PROPERTY_VALUE_INPUT)
    })
  })

  it("retains a rename and newer value while its guard waits across a body refresh", async () => {
    const response = wrapSdkResponse({ memoryTrackers: [] })
    let resolveNoteInfo!: (value: typeof response) => void
    const noteInfo = new Promise<typeof response>((resolve) => {
      resolveNoteInfo = resolve
    })
    const getNoteInfo = mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [],
    }).mockReturnValue(noteInfo)
    const wrapper = await h.mountEditor("---\ntopic: wiki\n---\n\nBody.", {
      noteId: 42,
      route: noteShowLocation(42),
    })
    const key = wrapper.find('[data-testid="rich-note-property-row-key-input"]')
    await key.trigger("focus")
    await key.setValue("domain")
    await key.trigger("blur")
    expect(getNoteInfo).toHaveBeenCalled()
    await h.setPropertyValueField(
      wrapper.find('[data-testid="rich-note-property-row-value-input"]'),
      "newer value"
    )
    await wrapper.setProps({
      modelValue: '---\ntopic: "wiki"\n---\n\nRefreshed body.',
    })
    expect(h.quillEditorEl().textContent).toContain("Refreshed body.")
    resolveNoteInfo(response)
    await flushPromises()
    expect(h.lastEmittedMarkdown()).toBe(
      "---\ndomain: newer value\n---\n\nRefreshed body."
    )
  })
})
