import { afterEach, beforeEach, vi } from "vitest"
import {
  advanceAnimationFrame,
  assertPresetOptionsVisible,
  INSERT_KEY_INPUT,
  keyInputValue,
  ROW_KEY_INPUT,
  selectPresetKey,
} from "./propertyKeyPresetsTestDom"
import { richModeKeyDropdownPresetKeysForPropertyRows } from "@/utils/noteContentFrontmatter"
import { propertyRowWithScalar } from "@/utils/noteContentPropertyRows"
import { preparePropertyKeyPresetDropdown } from "./propertyKeyPresetsTestSupport"
import { expectElementFocused } from "./propertyTouchFocusTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property key presets", () => {
  const h = createRichMarkdownEditorTestHarness()

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
  })

  afterEach(() => {
    h.cleanup()
    vi.useRealTimers()
  })

  it("inserting a property emits composed frontmatter and preserves body", async () => {
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

  it("offers available presets and sets keys for existing and inserted rows", async () => {
    const existingRows = [
      propertyRowWithScalar("custom", "workshop"),
      propertyRowWithScalar("image", "/x.png"),
    ]
    await preparePropertyKeyPresetDropdown(
      h,
      `---
custom: workshop
image: /x.png
---

# Body`,
      { keyInputTestId: ROW_KEY_INPUT, existingRows }
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
