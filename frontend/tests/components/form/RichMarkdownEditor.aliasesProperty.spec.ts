import { flushPromises } from "@vue/test-utils"
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
  clickListAdd,
  clickModeTab,
  dialogEl,
  mountPropertyValuePopup,
  popupValidationText,
  savePopup,
  setListItemValue,
  setTextareaValue,
} from "./propertyValuePopupTestDom"
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

  it("rejects invalid aliases in popup then saves a valid list", async () => {
    const wrapper = await mountPropertyValuePopup(h, ALIASES_LIST_MARKDOWN)

    clickModeTab("rich-note-property-value-popup-mode-text")
    await flushPromises()
    setTextareaValue("single alias")
    await savePopup()
    expect(popupValidationText()).toBe(AUTHORED_ALIASES_MESSAGE)
    expect(dialogEl()).not.toBeNull()
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()

    clickModeTab("rich-note-property-value-popup-mode-list")
    await flushPromises()
    setListItemValue(0, "bad|alias")
    await savePopup()
    expect(popupValidationText()).toBe(AUTHORED_ALIASES_MESSAGE)
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()

    setListItemValue(0, "color")
    clickListAdd()
    await flushPromises()
    setListItemValue(1, "hue")
    await savePopup()

    const last = h.lastEmittedMarkdown()
    expect(last).toMatch(/aliases:\s*\n\s*- color/)
    expect(last).toMatch(/- hue/)
    expect(dialogEl()).toBeNull()
  })

  it("inserts aliases as a list and blocks scalar aliases on row commit", async () => {
    await h.mountAndCommitInsertProperty("aliases", "color")

    expect(
      h
        .getWrapper()
        .find('[data-testid="rich-note-property-validation"]')
        .exists()
    ).toBe(false)
    const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties.aliases).toEqual(listPropertyValue(["color"]))

    const wrapper = h.getWrapper()
    await wrapper.setProps({ modelValue: ALIASES_SCALAR_MARKDOWN })
    await flushPromises()
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
