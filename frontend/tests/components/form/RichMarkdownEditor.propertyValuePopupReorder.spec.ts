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

  it("preserves reordered list items in composed YAML when saved from popup", async () => {
    const markdown = `---
tags:
  - alpha
  - beta
  - gamma
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    clickListMoveDown(0)
    await flushPromises()
    clickSave()
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    const alphaIdx = last.indexOf("- alpha")
    const betaIdx = last.indexOf("- beta")
    const gammaIdx = last.indexOf("- gamma")
    expect(betaIdx).toBeLessThan(alphaIdx)
    expect(alphaIdx).toBeLessThan(gammaIdx)
    expect(document.querySelector("dialog")).toBeNull()
  })

  it("reorders duplicate list items as distinct rows in popup", async () => {
    const markdown = `---
tags:
  - dup
  - dup
  - unique
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    clickListMoveUp(2)
    await flushPromises()
    clickSave()
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toMatch(/- dup\n\s*- unique\n\s*- dup/)
  })

  it("disables move up on first item and move down on last item", async () => {
    const markdown = `---
tags:
  - first
  - last
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    const moveUpFirst = document.querySelector(
      '[data-testid="rich-note-property-value-popup-list-move-up-0"]'
    ) as HTMLButtonElement
    const moveDownLast = document.querySelector(
      '[data-testid="rich-note-property-value-popup-list-move-down-1"]'
    ) as HTMLButtonElement
    expect(moveUpFirst.disabled).toBe(true)
    expect(moveDownLast.disabled).toBe(true)
  })
})
