import { flushPromises } from "@vue/test-utils"
import {
  clickListRemove,
  dialogEl,
  getTextareaValue,
  isListModeTabActive,
  openValuePopup,
  popupValidationText,
  savePopup,
  setListItemValue,
  setTextareaValue,
} from "./propertyValuePopupTestDom"
import {
  LIST_TOPIC_MARKDOWN,
  mountTopicValuePopup,
  switchToListMode,
  switchToTextMode,
  writeListItems,
} from "./propertyValuePopupModeSwitchTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property value popup mode switch", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("switches scalar↔list in popup, seeds text from list, and saves each mode", async () => {
    const wrapper = await mountTopicValuePopup(h)
    await switchToListMode()
    await writeListItems("workshop", "retreat")
    await savePopup()

    const asList = h.lastEmittedMarkdown()
    expect(asList).toMatch(/topic:\s*\n\s*- workshop/)
    expect(asList).toMatch(/- retreat/)
    expect(asList).toContain("Body")
    expect(dialogEl()).toBeNull()

    await wrapper.setProps({ modelValue: asList })
    await openValuePopup(wrapper)
    expect(isListModeTabActive()).toBe(true)

    await switchToTextMode()
    const seeded = getTextareaValue()
    expect(seeded).toContain("workshop")
    expect(seeded).toContain("retreat")

    setTextareaValue("combined value")
    await savePopup()

    const asScalar = h.lastEmittedMarkdown()
    expect(asScalar).toContain("topic: combined value")
    expect(asScalar).not.toMatch(/topic:\s*\n\s*-/)
    expect(dialogEl()).toBeNull()
  })

  it("allows duplicate list items and saves an emptied list from popup", async () => {
    const wrapper = await mountTopicValuePopup(h, LIST_TOPIC_MARKDOWN)
    setListItemValue(0, "dup")
    setListItemValue(1, "dup")
    await savePopup()
    expect(h.lastEmittedMarkdown()).toMatch(/- dup\n\s*- dup/)
    expect(dialogEl()).toBeNull()

    await wrapper.setProps({ modelValue: h.lastEmittedMarkdown() })
    await openValuePopup(wrapper)
    clickListRemove(1)
    clickListRemove(0)
    await flushPromises()
    await savePopup()
    expect(h.lastEmittedMarkdown()).toMatch(/topic:\s*\[\]/)
    expect(dialogEl()).toBeNull()
  })

  it("rejects empty list items on save", async () => {
    const wrapper = await mountTopicValuePopup(h, LIST_TOPIC_MARKDOWN)
    setListItemValue(1, "   ")
    await savePopup()

    expect(popupValidationText()).toContain("List items cannot be empty.")
    expect(dialogEl()).not.toBeNull()
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()
  })
})
