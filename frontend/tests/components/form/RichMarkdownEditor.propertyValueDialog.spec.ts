import { flushPromises } from "@vue/test-utils"
import {
  clickCancel,
  clickListMoveUp,
  clickListRemove,
  clickModeTab,
  clickSave,
  getTextareaValue,
  isListModeTabActive,
  listMoveButtonEl,
  modeTabEl,
  mountEditorOnNoteShow,
  mountPropertyValueDialog,
  openPropertyValueDialog,
  PROPERTY_VALUE_DIALOG_OPEN_SELECTOR,
  propertyValueDialogEl,
  propertyValueDialogValidationText,
  savePropertyValueDialog,
  setListItemValue,
  setTextareaValue,
} from "./propertyValueDialogTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const LIST_TOPIC_MARKDOWN = `---
topic:
  - alpha
  - beta
---

Body`

describe("RichMarkdownEditor property value dialog", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("cancel discards edits; reopen scalar-only key saves scalar YAML", async () => {
    const wrapper = await mountPropertyValueDialog(
      h,
      `---
image_mask: region-a
---

Body`
    )

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

  it.each([
    {
      case: "shows value edit icon on list property rows",
      markdown: `---
tags:
  - alpha
---

Body`,
      expectedCount: 1,
    },
    {
      case: "does not show value edit icon on specialized scalar property rows",
      markdown: `---
relation: related-to
wikidata_id: Q42
---

Body`,
      expectedCount: 0,
    },
  ])("$case", async ({ markdown, expectedCount }) => {
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()
    expect(wrapper.findAll(PROPERTY_VALUE_DIALOG_OPEN_SELECTOR)).toHaveLength(
      expectedCount
    )
  })

  describe("mode switch", () => {
    it("switches scalar↔list, seeds text from list, and saves list mode", async () => {
      await mountPropertyValueDialog(h, LIST_TOPIC_MARKDOWN)
      expect(isListModeTabActive()).toBe(true)

      clickModeTab("text")
      await flushPromises()
      const seeded = getTextareaValue()
      expect(seeded).toContain("alpha")
      expect(seeded).toContain("beta")

      clickModeTab("list")
      await flushPromises()
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

    it("rejects blank items but saves an empty list", async () => {
      const wrapper = await mountPropertyValueDialog(h, LIST_TOPIC_MARKDOWN)
      setListItemValue(1, "   ")
      await savePropertyValueDialog()

      expect(propertyValueDialogValidationText()).toContain(
        "List items cannot be empty."
      )
      expect(wrapper.emitted("update:modelValue")).toBeUndefined()

      clickListRemove(1)
      clickListRemove(0)
      await flushPromises()
      await savePropertyValueDialog()
      expect(h.lastEmittedMarkdown()).toMatch(/topic:\s*\[\]/)
      expect(propertyValueDialogEl()).toBeNull()
    })
  })

  describe("reorder", () => {
    it("disables edge moves, reorders items including duplicates, and saves YAML order", async () => {
      const markdown = `---
tags:
  - dup
  - dup
  - unique
---

Body`
      const wrapper = await mountEditorOnNoteShow(h, markdown)
      await openPropertyValueDialog(wrapper)

      const moveUpFirst = listMoveButtonEl("up", 0)
      const moveDownLast = listMoveButtonEl("down", 2)
      expect(moveUpFirst).not.toBeNull()
      expect(moveDownLast).not.toBeNull()
      expect(moveUpFirst!.disabled).toBe(true)
      expect(moveDownLast!.disabled).toBe(true)

      clickListMoveUp(2)
      await flushPromises()
      clickSave()
      await flushPromises()

      const reordered = h.lastEmittedMarkdown()
      expect(reordered).toMatch(/- dup\n\s*- unique\n\s*- dup/)
      expect(propertyValueDialogEl()).toBeNull()
    })
  })
})
