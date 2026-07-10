import { flushPromises, type VueWrapper } from "@vue/test-utils"
import {
  clickModeTab,
  openValuePopup,
  setListItemValue,
  setTextareaValue,
} from "./propertyValuePopupTestDom"
import type { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

type Harness = ReturnType<typeof createRichMarkdownEditorTestHarness>

export const ALIASES_LIST_MARKDOWN = `---
aliases:
  - color
---

Body`

export const ALIASES_SCALAR_MARKDOWN = `---
aliases: color
---

Body`

export async function mountAliasesValuePopup(
  h: Harness,
  markdown: string = ALIASES_LIST_MARKDOWN
): Promise<VueWrapper> {
  const wrapper = await h.mountEditor(markdown, { attachToBody: true })
  await openValuePopup(wrapper)
  return wrapper
}

export function propertyRowValidationText(wrapper: VueWrapper): string {
  return wrapper.find('[data-testid="rich-note-property-validation"]').text()
}

export async function addNewAliasesProperty(h: Harness, alias: string) {
  await h.mountEditor("# Body", { attachToBody: true })
  await h.openAddProperty()
  const w = h.getWrapper()
  const keyInput = w.find('[data-testid="rich-note-property-key"]')
  const valInput = w.find('[data-testid="rich-note-property-value"]')
  await keyInput.setValue("aliases")
  await h.setWikiPropertyValueField(valInput, alias)
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

export const POPUP_ALIAS_CONSTRAINT_CASES = [
  {
    case: "scalar text in popup",
    prepareInvalidValue: async () => {
      clickModeTab("rich-note-property-value-popup-mode-text")
      await flushPromises()
      setTextareaValue("single alias")
    },
    expectDialogOpen: true,
  },
  {
    case: "invalid list item in popup",
    prepareInvalidValue: async () => {
      setListItemValue(0, "bad|alias")
    },
    expectDialogOpen: false,
  },
] as const
