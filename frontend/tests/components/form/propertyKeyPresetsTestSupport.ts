import { richModeKeyDropdownPresetKeysForPropertyRows } from "@/utils/noteContentFrontmatter"
import type { PropertyRow } from "@/utils/noteContentPropertyRows"
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
