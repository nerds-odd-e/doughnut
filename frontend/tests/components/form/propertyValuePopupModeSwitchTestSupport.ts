import { flushPromises, type VueWrapper } from "@vue/test-utils"
import {
  clickListAdd,
  clickModeTab,
  openValuePopup,
  setListItemValue,
} from "./propertyValuePopupTestDom"
import type { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

export const SCALAR_TOPIC_MARKDOWN = `---
topic: training
---

Body`

export const LIST_TOPIC_MARKDOWN = `---
topic:
  - alpha
  - beta
---

Body`

type Harness = ReturnType<typeof createRichMarkdownEditorTestHarness>

export async function mountTopicValuePopup(
  h: Harness,
  markdown: string = SCALAR_TOPIC_MARKDOWN
): Promise<VueWrapper> {
  const wrapper = await h.mountEditor(markdown, { attachToBody: true })
  await openValuePopup(wrapper)
  return wrapper
}

export async function switchToListMode() {
  clickModeTab("rich-note-property-value-popup-mode-list")
  await flushPromises()
}

export async function switchToTextMode() {
  clickModeTab("rich-note-property-value-popup-mode-text")
  await flushPromises()
}

export async function writeListItems(...values: string[]) {
  for (let i = 0; i < values.length; i++) {
    if (i > 0) {
      clickListAdd()
      await flushPromises()
    }
    setListItemValue(i, values[i]!)
  }
}
