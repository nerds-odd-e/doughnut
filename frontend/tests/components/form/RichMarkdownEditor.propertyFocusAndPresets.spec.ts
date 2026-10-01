import { advanceAnimationFrame } from "@tests/helpers/focusTargetTestSupport"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"
import { richModeKeyDropdownPresetKeysForPropertyRows } from "@/utils/noteContentFrontmatter"
import { propertyRowWithScalar } from "@/utils/noteContentPropertyRows"
import { expectPresetOptions, selectPresetKey } from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const INSERT_KEY_INPUT = '[data-testid="rich-note-property-key"]'
const ROW_KEY_INPUT = '[data-testid="rich-note-property-row-key-input"]'
const ROW_VALUE_INPUT = '[data-testid="rich-note-property-row-value-input"]'

function inputEl(selector: string): HTMLInputElement {
  const el = document.querySelector(selector) as HTMLInputElement | null
  expect(el).not.toBeNull()
  return el!
}

function expectElementFocused(selector: string) {
  expect(document.activeElement).toBe(inputEl(selector))
}

describe("RichMarkdownEditor property focus and presets", () => {
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
      const existingKeyInput = inputEl(ROW_KEY_INPUT)
      existingKeyInput.focus()
      await nextTick()
      await flushPromises()
      expectPresetOptions(
        richModeKeyDropdownPresetKeysForPropertyRows(false, [
          propertyRowWithScalar("custom", "workshop"),
          propertyRowWithScalar("image", "/x.png"),
        ])
      )

      await selectPresetKey("url")
      expect(existingKeyInput.value).toBe("url")
      expectElementFocused(`[data-property-key="url"] ${ROW_VALUE_INPUT}`)

      await h.openAddProperty()
      await advanceAnimationFrame()
      expectPresetOptions(
        richModeKeyDropdownPresetKeysForPropertyRows(false, [
          propertyRowWithScalar("image", "/x.png"),
          propertyRowWithScalar("url", "workshop"),
        ])
      )
      await selectPresetKey("wikidata_id")
      expect(inputEl(INSERT_KEY_INPUT).value).toBe("wikidata_id")
      expectElementFocused(
        '[data-testid="rich-note-wikidata-property-insert-edit"]'
      )
    })
  })

  describe("key preset narrowing", () => {
    async function typeKey(selector: string, text: string) {
      const el = inputEl(selector)
      el.value = text
      el.dispatchEvent(new Event("input", { bubbles: true }))
      await flushPromises()
    }

    async function openAddFormKey() {
      await h.mountEditor("# Body", { attachToBody: true })
      await h.openAddProperty()
      await advanceAnimationFrame()
    }

    it.each([
      { typed: "ur", listed: ["url"] },
      { typed: "UR", listed: ["url"] },
      { typed: "of", listed: ["example of"] },
      { typed: "mo", listed: [] },
    ])("add form key typed $typed lists $listed", async ({ typed, listed }) => {
      await openAddFormKey()
      await typeKey(INSERT_KEY_INPUT, typed)
      expectPresetOptions(listed)
    })

    it("add form lists every available preset once the typed text is cleared", async () => {
      await openAddFormKey()
      const all = richModeKeyDropdownPresetKeysForPropertyRows(false, [])
      await typeKey(INSERT_KEY_INPUT, "ur")
      await typeKey(INSERT_KEY_INPUT, "")
      expectPresetOptions(all)
    })

    it("existing row lists every available preset on focus and narrows on typing", async () => {
      await h.mountEditor("---\ncustom: workshop\n---\n\n# Body", {
        attachToBody: true,
      })
      inputEl(ROW_KEY_INPUT).focus()
      await flushPromises()
      expectPresetOptions(
        richModeKeyDropdownPresetKeysForPropertyRows(false, [
          propertyRowWithScalar("custom", "workshop"),
        ])
      )

      await typeKey(ROW_KEY_INPUT, "ur")
      expectPresetOptions(["url"])
    })

    it("choosing a narrowed preset closes the list and focuses the row value", async () => {
      await h.mountEditor("---\ncustom: workshop\n---\n\n# Body", {
        attachToBody: true,
      })
      inputEl(ROW_KEY_INPUT).focus()
      await flushPromises()
      await typeKey(ROW_KEY_INPUT, "ur")
      await selectPresetKey("url")
      expectPresetOptions([])
      expectElementFocused(`[data-property-key="url"] ${ROW_VALUE_INPUT}`)
    })
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
