import { flushPromises } from "@vue/test-utils"
import {
  clickListRemove,
  propertyValueDialogEl,
  getTextareaValue,
  isListModeTabActive,
  propertyValueDialogValidationText,
  savePropertyValueDialog,
  setListItemValue,
} from "./propertyValueDialogTestDom"
import {
  LIST_TOPIC_MARKDOWN,
  mountTopicValueDialog,
  switchToListMode,
  switchToTextMode,
} from "./propertyValueDialogModeSwitchTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property value dialog mode switch", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("switches scalar↔list, seeds text from list, and saves list mode", async () => {
    await mountTopicValueDialog(h, LIST_TOPIC_MARKDOWN)
    expect(isListModeTabActive()).toBe(true)

    await switchToTextMode()
    const seeded = getTextareaValue()
    expect(seeded).toContain("alpha")
    expect(seeded).toContain("beta")

    await switchToListMode()
    setListItemValue(0, "workshop")
    setListItemValue(1, "retreat")
    await flushPromises()
    await savePropertyValueDialog()

    const asList = h.lastEmittedMarkdown()
    expect(asList).toMatch(/topic:\s*\n\s*- workshop/)
    expect(asList).toMatch(/- retreat/)
    expect(asList).toContain("Body")
    expect(propertyValueDialogEl()).toBeNull()
  })

  it("saves an emptied list from the property value dialog", async () => {
    await mountTopicValueDialog(h, LIST_TOPIC_MARKDOWN)
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
