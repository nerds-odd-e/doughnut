import { flushPromises } from "@vue/test-utils"
import {
  clickCancel,
  clickSave,
  getTextareaValue,
  modeTabEl,
  openPropertyValueDialog,
  propertyValueDialogEl,
  setTextareaValue,
} from "./propertyValueDialogTestDom"
import {
  EDIT_ICON_VISIBILITY_CASES,
  mountEditorAndCountEditIcons,
  mountImageMaskValueDialog,
} from "./propertyValueDialogTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property value dialog", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("cancel discards edits; reopen scalar-only key saves scalar YAML", async () => {
    const wrapper = await mountImageMaskValueDialog(h)

    expect(modeTabEl("list")).toBeNull()
    expect(modeTabEl("text")).not.toBeNull()

    setTextareaValue("changed but not saved")
    clickCancel()
    await flushPromises()

    expect(wrapper.emitted("update:modelValue")).toBeUndefined()
    expect(propertyValueDialogEl()).toBeNull()

    await openPropertyValueDialog(wrapper)
    expect(getTextareaValue()).toBe("region-a")
    setTextareaValue("region-b")
    clickSave()
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("image_mask: region-b")
    expect(last).not.toMatch(/image_mask:\s*\n\s*-/)
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
