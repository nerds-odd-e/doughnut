import { keyInputValue, selectPresetKey } from "./propertyKeyPresetsTestDom"
import {
  PRESET_DROPDOWN_CASES,
  preparePropertyKeyPresetDropdown,
} from "./propertyKeyPresetsTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property key presets", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("inserting a property emits composed frontmatter and preserves body", async () => {
    await h.mountEditor("# Hello Body")
    await h.openAddProperty()

    const keyInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-key"]')
    const valInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-value"]')
    await keyInput.setValue("status")
    await h.setWikiPropertyValueField(valInput, "draft")
    await valInput.trigger("blur")

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("---")
    expect(last).toContain("status: draft")
    expect(last).toContain("Hello Body")
  })

  it.each(PRESET_DROPDOWN_CASES)(
    "preset dropdown for $case shows options and sets key on selection",
    async ({
      markdown,
      keyInputTestId,
      existingRows,
      selectPreset,
      expectedKeyValue,
    }) => {
      await preparePropertyKeyPresetDropdown(h, markdown, {
        keyInputTestId,
        existingRows,
      })
      await selectPresetKey(selectPreset)
      expect(keyInputValue(keyInputTestId)).toBe(expectedKeyValue)
    }
  )
})
