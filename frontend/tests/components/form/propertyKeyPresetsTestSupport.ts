import { richModeKeyDropdownPresetKeysForPropertyRows } from "@/utils/noteContentFrontmatter"
import {
  propertyRowWithScalar,
  type PropertyRow,
} from "@/utils/noteContentPropertyRows"
import {
  advanceAnimationFrame,
  assertPresetOptionsVisible,
  focusKeyInput,
  INSERT_KEY_INPUT,
  ROW_KEY_INPUT,
} from "./propertyKeyPresetsTestDom"
import type { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

type Harness = ReturnType<typeof createRichMarkdownEditorTestHarness>

export async function preparePropertyKeyPresetDropdown(
  h: Harness,
  markdown: string,
  options: {
    keyInputTestId: typeof INSERT_KEY_INPUT | typeof ROW_KEY_INPUT
    existingRows: readonly PropertyRow[]
  }
) {
  await h.mountEditor(markdown, { attachToBody: true })
  if (options.keyInputTestId === INSERT_KEY_INPUT) {
    await h.openAddProperty()
    await advanceAnimationFrame()
  } else {
    await focusKeyInput(options.keyInputTestId)
  }
  assertPresetOptionsVisible(
    richModeKeyDropdownPresetKeysForPropertyRows(false, options.existingRows)
  )
}

export const PRESET_DROPDOWN_CASES = [
  {
    case: "insert row",
    markdown: "# Hello Body",
    keyInputTestId: INSERT_KEY_INPUT,
    existingRows: [] as PropertyRow[],
    selectPreset: "wikidata_id",
    expectedKeyValue: "wikidata_id",
    expectedFocusTestId: "rich-note-wikidata-property-insert-edit",
  },
  {
    case: "existing row",
    markdown: `---
status: ok
---

# Body`,
    keyInputTestId: ROW_KEY_INPUT,
    existingRows: [propertyRowWithScalar("status", "ok")],
    selectPreset: "url",
    expectedKeyValue: "url",
    expectedFocusTestId: "rich-note-property-row-value-input",
  },
  {
    case: "occupied image preset",
    markdown: `---
image: /x.png
---

# Body`,
    keyInputTestId: INSERT_KEY_INPUT,
    existingRows: [propertyRowWithScalar("image", "/x.png")],
    selectPreset: "image 2",
    expectedKeyValue: "image 2",
    expectedFocusTestId: "rich-note-property-value",
  },
] as const
