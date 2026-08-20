import { flushPromises } from "@vue/test-utils"
import {
  clickListMoveDown,
  clickListMoveUp,
  clickSave,
  openValuePopup,
} from "./propertyValuePopupTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property value popup reorder", () => {
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
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    const moveUpFirst = document.querySelector(
      '[data-testid="rich-note-property-value-popup-list-move-up-0"]'
    ) as HTMLButtonElement
    const moveDownLast = document.querySelector(
      '[data-testid="rich-note-property-value-popup-list-move-down-2"]'
    ) as HTMLButtonElement
    expect(moveUpFirst.disabled).toBe(true)
    expect(moveDownLast.disabled).toBe(true)

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
    expect(document.querySelector("dialog")).toBeNull()

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
    await openValuePopup(wrapper)

    clickListMoveUp(2)
    await flushPromises()
    clickSave()
    await flushPromises()

    expect(h.lastEmittedMarkdown()).toMatch(/- dup\n\s*- unique\n\s*- dup/)
  })
})
