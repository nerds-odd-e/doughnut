import { flushPromises, type VueWrapper } from "@vue/test-utils"
import {
  openPropertyValueDialog,
  mountEditorOnNoteShow,
  PROPERTY_VALUE_DIALOG_OPEN_SELECTOR,
} from "./propertyValueDialogTestDom"
import {
  mountTopicValueDialog,
  SCALAR_TOPIC_MARKDOWN,
} from "./propertyValueDialogModeSwitchTestSupport"
import type { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

export { mountTopicValueDialog, SCALAR_TOPIC_MARKDOWN }

type Harness = ReturnType<typeof createRichMarkdownEditorTestHarness>

export const IMAGE_MASK_MARKDOWN = `---
image_mask: region-a
---

Body`

export const LIST_TAGS_MARKDOWN = `---
tags:
  - alpha
---

Body`

export const SPECIALIZED_SCALAR_MARKDOWN = `---
relation: related-to
wikidata_id: Q42
---

Body`

export async function mountImageMaskValueDialog(
  h: Harness
): Promise<VueWrapper> {
  const wrapper = await mountEditorOnNoteShow(h, IMAGE_MASK_MARKDOWN)
  await openPropertyValueDialog(wrapper)
  return wrapper
}

export const EDIT_ICON_VISIBILITY_CASES = [
  {
    case: "shows value edit icon on list property rows",
    markdown: LIST_TAGS_MARKDOWN,
    expectedCount: 1,
  },
  {
    case: "does not show value edit icon on specialized scalar property rows",
    markdown: SPECIALIZED_SCALAR_MARKDOWN,
    expectedCount: 0,
  },
] as const

export async function mountEditorAndCountEditIcons(
  h: Harness,
  markdown: string
): Promise<number> {
  const wrapper = await h.mountEditor(markdown)
  await flushPromises()
  return wrapper.findAll(PROPERTY_VALUE_DIALOG_OPEN_SELECTOR).length
}
