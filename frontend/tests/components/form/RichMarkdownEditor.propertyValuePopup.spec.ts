import { flushPromises } from "@vue/test-utils"
import {
  clickCancel,
  clickSave,
  dialogEl,
  getTextareaValue,
  isModeTabActive,
  modeTabEl,
  setTextareaValue,
} from "./propertyValuePopupTestDom"
import {
  EDIT_ICON_VISIBILITY_CASES,
  mountEditorAndCountEditIcons,
  mountImageMaskValuePopup,
  mountTopicValuePopup,
} from "./propertyValuePopupTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property value popup", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("saves edited scalar value from popup without changing YAML shape to a list", async () => {
    await mountTopicValuePopup(h)

    expect(dialogEl()).not.toBeNull()
    expect(isModeTabActive("rich-note-property-value-popup-mode-text")).toBe(
      true
    )
    expect(getTextareaValue()).toBe("training")

    setTextareaValue("advanced workshop")
    await flushPromises()
    clickSave()
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("topic: advanced workshop")
    expect(last).not.toMatch(/topic:\s*\n\s*-/)
    expect(last).toContain("Body")
    expect(dialogEl()).toBeNull()
  })

  it("cancel closes popup without emitting property changes", async () => {
    const wrapper = await mountTopicValuePopup(h)

    setTextareaValue("changed but not saved")
    await flushPromises()

    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0

    clickCancel()
    await flushPromises()

    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )
    expect(dialogEl()).toBeNull()

    const valField = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    expect(valField.text()).toContain("training")
  })

  it("hides list mode for scalar-only structural keys", async () => {
    await mountImageMaskValuePopup(h)

    expect(modeTabEl("rich-note-property-value-popup-mode-list")).toBeNull()
    expect(modeTabEl("rich-note-property-value-popup-mode-text")).not.toBeNull()
  })

  it.each(EDIT_ICON_VISIBILITY_CASES)("$case", async ({
    markdown,
    expectedCount,
  }) => {
    expect(await mountEditorAndCountEditIcons(h, markdown)).toBe(expectedCount)
  })
})
