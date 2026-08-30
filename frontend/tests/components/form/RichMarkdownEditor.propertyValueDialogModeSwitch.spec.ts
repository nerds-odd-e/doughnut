import { flushPromises } from "@vue/test-utils"
import {
  clickListRemove,
  propertyValueDialogEl,
  getTextareaValue,
  isListModeTabActive,
  openPropertyValueDialog,
  propertyValueDialogValidationText,
  savePropertyValueDialog,
  setListItemValue,
  setTextareaValue,
} from "./propertyValueDialogTestDom"
import {
  LIST_TOPIC_MARKDOWN,
  mountTopicValueDialog,
  switchToListMode,
  switchToTextMode,
  writeListItems,
} from "./propertyValueDialogModeSwitchTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property value dialog mode switch", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("switches scalar↔list in the property value dialog, seeds text from list, and saves each mode", async () => {
    const wrapper = await mountTopicValueDialog(h)
    await switchToListMode()
    await writeListItems("workshop", "retreat")
    await savePropertyValueDialog()

    const asList = h.lastEmittedMarkdown()
    expect(asList).toMatch(/topic:\s*\n\s*- workshop/)
    expect(asList).toMatch(/- retreat/)
    expect(asList).toContain("Body")
    expect(propertyValueDialogEl()).toBeNull()

    await wrapper.setProps({ modelValue: asList })
    await openPropertyValueDialog(wrapper)
    expect(isListModeTabActive()).toBe(true)

    await switchToTextMode()
    const seeded = getTextareaValue()
    expect(seeded).toContain("workshop")
    expect(seeded).toContain("retreat")

    setTextareaValue("combined value")
    await savePropertyValueDialog()

    const asScalar = h.lastEmittedMarkdown()
    expect(asScalar).toContain("topic: combined value")
    expect(asScalar).not.toMatch(/topic:\s*\n\s*-/)
    expect(propertyValueDialogEl()).toBeNull()
  })

  it("allows duplicate list items and saves an emptied list from the property value dialog", async () => {
    const wrapper = await mountTopicValueDialog(h, LIST_TOPIC_MARKDOWN)
    setListItemValue(0, "dup")
    setListItemValue(1, "dup")
    await savePropertyValueDialog()
    expect(h.lastEmittedMarkdown()).toMatch(/- dup\n\s*- dup/)
    expect(propertyValueDialogEl()).toBeNull()

    await wrapper.setProps({ modelValue: h.lastEmittedMarkdown() })
    await openPropertyValueDialog(wrapper)
    clickListRemove(1)
    clickListRemove(0)
    await flushPromises()
    await savePropertyValueDialog()
    expect(h.lastEmittedMarkdown()).toMatch(/topic:\s*\[\]/)
    expect(propertyValueDialogEl()).toBeNull()
  })

  it("rejects empty list items on save", async () => {
    const wrapper = await mountTopicValueDialog(h, LIST_TOPIC_MARKDOWN)
    setListItemValue(1, "   ")
    await savePropertyValueDialog()

    expect(propertyValueDialogValidationText()).toContain(
      "List items cannot be empty."
    )
    expect(propertyValueDialogEl()).not.toBeNull()
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()
  })
})
