import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { deadWikiLinkInPropertyValueEl } from "./propertiesTestDom"
import type { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

type Harness = ReturnType<typeof createRichMarkdownEditorTestHarness>

export const DUPLICATE_KEYS_MARKDOWN = `---
alpha: one
beta: two
---

Body`

export function propertyWikiLinkMarkdown(wikiToken: string): string {
  return `---
topic: "[[${wikiToken}]]"
---

Body`
}

export const DEAD_LINK_CLICK_CASES = [
  {
    case: "plain wiki token",
    wikiToken: "Missing Note",
    expected: { targetToken: "Missing Note", displayText: "Missing Note" },
  },
  {
    case: "display text",
    wikiToken: "Ghost Page|shown text",
    expected: { targetToken: "Ghost Page", displayText: "shown text" },
  },
] as const

export async function clickDeadWikiLinkInPropertyValue(wrapper: VueWrapper) {
  deadWikiLinkInPropertyValueEl(wrapper.element).click()
  await flushPromises()
}

export async function attemptRenamePropertyKey(
  wrapper: VueWrapper,
  rowIndex: number,
  newKey: string
) {
  const rows = wrapper.findAll('[data-testid="rich-note-property-row"]')
  const keyInput = rows[rowIndex]!.find(
    '[data-testid="rich-note-property-row-key-input"]'
  )
  await keyInput.trigger("focus")
  await keyInput.setValue(newKey)
  await keyInput.trigger("blur")
  await flushPromises()
}

export async function mountDuplicateKeysEditor(h: Harness) {
  return h.mountEditor(DUPLICATE_KEYS_MARKDOWN)
}
