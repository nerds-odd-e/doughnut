import { flushPromises, type VueWrapper } from "@vue/test-utils"
import {
  clickModeTab,
  openValuePopup,
  setListItemValue,
  setTextareaValue,
} from "./propertyValuePopupTestDom"
import type { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

type Harness = ReturnType<typeof createRichMarkdownEditorTestHarness>

export const OVERLAPS_LIST_MARKDOWN = `---
overlaps:
  - "[[Other Note]]"
---

Body`

export const OVERLAPS_SCALAR_MARKDOWN = `---
overlaps: "[[Other Note]]"
---

Body`

export async function mountOverlapsValuePopup(
  h: Harness,
  markdown: string = OVERLAPS_LIST_MARKDOWN
): Promise<VueWrapper> {
  const wrapper = await h.mountEditor(markdown, { attachToBody: true })
  await openValuePopup(wrapper)
  return wrapper
}

export function propertyRowValidationText(wrapper: VueWrapper): string {
  return wrapper.find('[data-testid="rich-note-property-validation"]').text()
}

export async function addNewOverlapsProperty(h: Harness, wikiLink: string) {
  await h.mountEditor("# Body", { attachToBody: true })
  await h.openAddProperty()
  const w = h.getWrapper()
  const keyInput = w.find('[data-testid="rich-note-property-key"]')
  const valInput = w.find('[data-testid="rich-note-property-value"]')
  await keyInput.setValue("overlaps")
  await h.setPropertyValueField(valInput, wikiLink)
  await valInput.trigger("blur")
  await flushPromises()
}

export async function triggerRowKeyBlurValidation(wrapper: VueWrapper) {
  const keyInput = wrapper.find(
    '[data-testid="rich-note-property-row-key-input"]'
  )
  await keyInput.trigger("focus")
  await keyInput.trigger("blur")
  await flushPromises()
}

export const POPUP_OVERLAPS_CONSTRAINT_CASES = [
  {
    case: "scalar text in popup",
    prepareInvalidValue: async () => {
      clickModeTab("rich-note-property-value-popup-mode-text")
      await flushPromises()
      setTextareaValue("[[Other Note]]")
    },
    expectDialogOpen: true,
  },
  {
    case: "plain list item in popup",
    prepareInvalidValue: async () => {
      setListItemValue(0, "plain alias")
    },
    expectDialogOpen: false,
  },
] as const
