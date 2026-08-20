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

  it("saves scalar as list when user switches to list mode in popup", async () => {
    await mountTopicValuePopup(h)
    await switchToListMode()
    await writeListItems("workshop", "retreat")
    await savePopup()

    const last = h.lastEmittedMarkdown()
    expect(last).toMatch(/topic:\s*\n\s*- workshop/)
    expect(last).toMatch(/- retreat/)
    expect(last).toContain("Body")
    expect(dialogEl()).toBeNull()
  })

  it("seeds text mode from populated list when switching from list mode", async () => {
    await mountTopicValuePopup(h, LIST_TOPIC_MARKDOWN)
    await switchToTextMode()

    const textareaValue = getTextareaValue()
    expect(textareaValue).toContain("alpha")
    expect(textareaValue).toContain("beta")

    await savePopup()

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("topic: alpha, beta")
    expect(last).not.toMatch(/topic:\s*\n\s*-/)
    expect(dialogEl()).toBeNull()
  })

  it("saves list as scalar when user switches to text mode in popup", async () => {
    await mountTopicValuePopup(h, LIST_TOPIC_MARKDOWN)

    expect(isListModeTabActive()).toBe(true)

    await switchToTextMode()
    setTextareaValue("combined value")
    await savePopup()

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("topic: combined value")
    expect(last).not.toMatch(/topic:\s*\n\s*-/)
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
