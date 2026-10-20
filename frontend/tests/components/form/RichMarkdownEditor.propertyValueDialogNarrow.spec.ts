import { flushPromises } from "@vue/test-utils"
import {
  clickCancel,
  clickListMoveUp,
  mountPropertyValueDialog,
  openPropertyValueDialog,
  propertyValueDialogEl,
  propertyValueDialogValidationText,
  savePropertyValueDialog,
  setListItemValue,
} from "./propertyValueDialogTestDom"
import {
  atViewport,
  interactiveControls,
  rect,
} from "./propertyLayoutTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const markdown = `---
topic:
  - alpha
  - beta
  - alpha
---

Body`

function expectDialogControlsWithinWindow() {
  const controls = interactiveControls(propertyValueDialogEl()!)
  expect(controls.length).toBeGreaterThan(5)
  for (const control of controls) {
    const r = rect(control)
    expect(r.width).toBeGreaterThan(0)
    expect(r.left).toBeGreaterThanOrEqual(0)
    expect(r.right).toBeLessThanOrEqual(window.innerWidth)
  }
}

describe("RichMarkdownEditor property value dialog in a narrow window", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("reorders repeated values, corrects an invalid item, saves, and cancels a later edit", async () => {
    await atViewport(320)
    const wrapper = await mountPropertyValueDialog(h, markdown)
    expectDialogControlsWithinWindow()

    clickListMoveUp(1)
    setListItemValue(2, "  ")
    await savePropertyValueDialog()
    expect(propertyValueDialogValidationText()).toContain(
      "List items cannot be empty."
    )
    expectDialogControlsWithinWindow()
    setListItemValue(2, "alpha")
    await savePropertyValueDialog()

    expect(propertyValueDialogEl()).toBeNull()
    const saved = h.lastEmittedMarkdown()
    expect(saved).toMatch(/topic:\s*\n\s*- beta\n\s*- alpha\n\s*- alpha/)

    await openPropertyValueDialog(wrapper)
    setListItemValue(0, "gamma")
    clickCancel()
    await flushPromises()

    expect(propertyValueDialogEl()).toBeNull()
    expect(h.lastEmittedMarkdown()).toBe(saved)
  })
})
