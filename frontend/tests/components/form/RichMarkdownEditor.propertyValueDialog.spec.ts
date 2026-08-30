import { flushPromises } from "@vue/test-utils"
import {
  clickCancel,
  clickSave,
  getTextareaValue,
  isModeTabActive,
  modeTabEl,
  openPropertyValueDialog,
  propertyValueDialogEl,
  setTextareaValue,
} from "./propertyValueDialogTestDom"
import {
  EDIT_ICON_VISIBILITY_CASES,
  mountEditorAndCountEditIcons,
  mountImageMaskValueDialog,
  mountTopicValueDialog,
} from "./propertyValueDialogTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property value dialog", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("cancel discards edits; reopen save keeps scalar YAML shape", async () => {
    const wrapper = await mountTopicValueDialog(h)

    expect(propertyValueDialogEl()).not.toBeNull()
    expect(isModeTabActive("text")).toBe(true)
    expect(getTextareaValue()).toBe("training")

    setTextareaValue("changed but not saved")
    await flushPromises()
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0
    clickCancel()
    await flushPromises()

    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )
    expect(propertyValueDialogEl()).toBeNull()
    expect(
      wrapper.find('[data-testid="rich-note-property-row-value-input"]').text()
    ).toContain("training")

    await openPropertyValueDialog(wrapper)
    expect(getTextareaValue()).toBe("training")
    setTextareaValue("advanced workshop")
    await flushPromises()
    clickSave()
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("topic: advanced workshop")
    expect(last).not.toMatch(/topic:\s*\n\s*-/)
    expect(last).toContain("Body")
    expect(propertyValueDialogEl()).toBeNull()
  })

  it("hides list mode for scalar-only structural keys", async () => {
    await mountImageMaskValueDialog(h)

    expect(modeTabEl("list")).toBeNull()
    expect(modeTabEl("text")).not.toBeNull()
  })

  it.each(EDIT_ICON_VISIBILITY_CASES)(
    "$case",
    async ({ markdown, expectedCount }) => {
      expect(await mountEditorAndCountEditIcons(h, markdown)).toBe(
        expectedCount
      )
    }
  )
})
