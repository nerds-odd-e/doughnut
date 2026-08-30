import { flushPromises } from "@vue/test-utils"
import {
  clickListMoveDown,
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
  - alpha
  - beta
  - gamma
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

    clickListMoveDown(0)
    await flushPromises()
    clickSave()
    await flushPromises()

    const reordered = h.lastEmittedMarkdown()
    const alphaIdx = reordered.indexOf("- alpha")
    const betaIdx = reordered.indexOf("- beta")
    const gammaIdx = reordered.indexOf("- gamma")
    expect(betaIdx).toBeLessThan(alphaIdx)
    expect(alphaIdx).toBeLessThan(gammaIdx)
    expect(propertyValueDialogEl()).toBeNull()

    await wrapper.setProps({
      modelValue: `---
tags:
  - dup
  - dup
  - unique
---

Body`,
    })
    await flushPromises()
    await openPropertyValueDialog(wrapper)

    clickListMoveUp(2)
    await flushPromises()
    clickSave()
    await flushPromises()

    expect(h.lastEmittedMarkdown()).toMatch(/- dup\n\s*- unique\n\s*- dup/)
  })
})
