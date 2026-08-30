import { flushPromises, type VueWrapper } from "@vue/test-utils"
import {
  clickListAdd,
  clickModeTab,
  mountEditorOnNoteShow,
  openPropertyValueDialog,
  setListItemValue,
} from "./propertyValueDialogTestDom"
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

export async function mountTopicValueDialog(
  h: Harness,
  markdown: string = SCALAR_TOPIC_MARKDOWN
): Promise<VueWrapper> {
  const wrapper = await mountEditorOnNoteShow(h, markdown)
  await openPropertyValueDialog(wrapper)
  return wrapper
}

export async function switchToListMode() {
  clickModeTab("list")
  await flushPromises()
}

export async function switchToTextMode() {
  clickModeTab("text")
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
