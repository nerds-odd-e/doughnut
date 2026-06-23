import { flushPromises } from "@vue/test-utils"
import {
  clickListAdd,
  clickListRemove,
  clickModeTab,
  clickSave,
  getTextareaValue,
  openValuePopup,
  setListItemValue,
  setTextareaValue,
} from "./propertyValuePopupTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property value popup mode switch", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("saves scalar as list when user switches to list mode in popup", async () => {
    const markdown = `---
topic: training
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    clickModeTab("rich-note-property-value-popup-mode-list")
    await flushPromises()
    setListItemValue(0, "workshop")
    clickListAdd()
    await flushPromises()
    setListItemValue(1, "retreat")
    clickSave()
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toMatch(/topic:\s*\n\s*- workshop/)
    expect(last).toMatch(/- retreat/)
    expect(last).toContain("Body")
    expect(document.querySelector("dialog")).toBeNull()
  })

  it("seeds text mode from populated list when switching from list mode", async () => {
    const markdown = `---
topic:
  - alpha
  - beta
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    clickModeTab("rich-note-property-value-popup-mode-text")
    await flushPromises()

    const textareaValue = getTextareaValue()
    expect(textareaValue).not.toBe("")
    expect(textareaValue).toContain("alpha")
    expect(textareaValue).toContain("beta")

    clickSave()
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("topic: alpha, beta")
    expect(last).not.toMatch(/topic:\s*\n\s*-/)
    expect(document.querySelector("dialog")).toBeNull()
  })

  it("saves list as scalar when user switches to text mode in popup", async () => {
    const markdown = `---
topic:
  - alpha
  - beta
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    expect(
      document
        .querySelector(
          '[data-testid="rich-note-property-value-popup-mode-list"]'
        )
        ?.classList.contains("daisy-tab-active")
    ).toBe(true)

    clickModeTab("rich-note-property-value-popup-mode-text")
    await flushPromises()
    setTextareaValue("combined value")
    clickSave()
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("topic: combined value")
    expect(last).not.toMatch(/topic:\s*\n\s*-/)
    expect(document.querySelector("dialog")).toBeNull()
  })

  it("allows duplicate list items in popup save", async () => {
    const markdown = `---
topic: training
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    clickModeTab("rich-note-property-value-popup-mode-list")
    await flushPromises()
    setListItemValue(0, "dup")
    clickListAdd()
    await flushPromises()
    setListItemValue(1, "dup")
    clickSave()
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toMatch(/- dup\n\s*- dup/)
  })

  it("rejects empty list items on save", async () => {
    const markdown = `---
topic: training
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    clickModeTab("rich-note-property-value-popup-mode-list")
    await flushPromises()
    setListItemValue(0, "valid")
    clickListAdd()
    await flushPromises()
    setListItemValue(1, "   ")
    clickSave()
    await flushPromises()

    expect(
      document.querySelector(
        '[data-testid="rich-note-property-value-popup-validation"]'
      )?.textContent
    ).toContain("List items cannot be empty.")
    expect(document.querySelector("dialog")).not.toBeNull()
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()
  })

  it("saves an empty list from popup", async () => {
    const markdown = `---
topic: training
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    clickModeTab("rich-note-property-value-popup-mode-list")
    await flushPromises()
    clickListRemove(0)
    await flushPromises()
    clickSave()
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toMatch(/topic:\s*\[\]/)
    expect(document.querySelector("dialog")).toBeNull()
  })
})
