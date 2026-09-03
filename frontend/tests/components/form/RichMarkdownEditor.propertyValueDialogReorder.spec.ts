import { flushPromises } from "@vue/test-utils"
import {
  clickListMoveUp,
  clickSave,
  listMoveButtonEl,
  mountEditorOnNoteShow,
  openPropertyValueDialog,
  propertyValueDialogEl,
} from "./propertyValueDialogTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property value dialog reorder", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

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
