import { AUTHORED_ALIASES_MESSAGE } from "@/utils/authoredAliasesValidation"
import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import {
  propertyValidationText,
  triggerRowKeyBlurValidation,
} from "./propertiesTestDom"
import {
  propertyValueDialogEl,
  mountPropertyValueDialog,
  propertyValueDialogValidationText,
  savePropertyValueDialog,
  setListItemValue,
} from "./propertyValueDialogTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const ALIASES_LIST_MARKDOWN = `---
aliases:
  - color
---

Body`

const ALIASES_SCALAR_MARKDOWN = `---
aliases: color
---

Body`

describe("RichMarkdownEditor aliases property", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("rejects an invalid alias in the property value dialog then saves a valid list", async () => {
    const wrapper = await mountPropertyValueDialog(h, ALIASES_LIST_MARKDOWN)

    setListItemValue(0, "bad|alias")
    await savePropertyValueDialog()
    expect(propertyValueDialogValidationText()).toBe(AUTHORED_ALIASES_MESSAGE)
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()

    setListItemValue(0, "hue")
    await savePropertyValueDialog()

    const last = h.lastEmittedMarkdown()
    expect(last).toMatch(/aliases:\s*\n\s*- hue/)
    expect(propertyValueDialogEl()).toBeNull()
  })

  it("inserts aliases as a list and blocks scalar aliases on row commit", async () => {
    await h.mountEditor("# Body")
    await h.commitInsertProperty("aliases", "color")

    const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties.aliases).toEqual(listPropertyValue(["color"]))

    const wrapper = h.getWrapper()
    await wrapper.setProps({ modelValue: ALIASES_SCALAR_MARKDOWN })
    const emissionsBeforeBlur =
      wrapper.emitted("update:modelValue")?.length ?? 0
    await triggerRowKeyBlurValidation(wrapper)

    expect(propertyValidationText(wrapper.element)).toBe(
      AUTHORED_ALIASES_MESSAGE
    )
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emissionsBeforeBlur
    )
  })
})
