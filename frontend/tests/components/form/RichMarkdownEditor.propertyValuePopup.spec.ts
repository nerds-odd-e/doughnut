import { flushPromises } from "@vue/test-utils"
import {
  clickSave,
  openValuePopup,
  setTextareaValue,
} from "./propertyValuePopupTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property value popup", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("opens dialog with text mode and textarea when edit icon is clicked", async () => {
    const markdown = `---
topic: training
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    const dialog = document.querySelector("dialog")
    expect(dialog).not.toBeNull()
    expect(
      document
        .querySelector(
          '[data-testid="rich-note-property-value-popup-mode-text"]'
        )
        ?.classList.contains("daisy-tab-active")
    ).toBe(true)
    const textarea = document.querySelector(
      '[data-testid="rich-note-property-value-popup-textarea"]'
    ) as HTMLTextAreaElement
    expect(textarea).not.toBeNull()
    expect(textarea.value).toBe("training")
  })

  it("saves edited scalar value from popup without changing YAML shape to a list", async () => {
    const markdown = `---
topic: training
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    setTextareaValue("advanced workshop")
    await flushPromises()
    clickSave()
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("topic: advanced workshop")
    expect(last).not.toMatch(/topic:\s*\n\s*-/)
    expect(last).toContain("Body")
    expect(document.querySelector("dialog")).toBeNull()
  })

  it("cancel closes popup without emitting property changes", async () => {
    const markdown = `---
topic: training
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    setTextareaValue("changed but not saved")
    await flushPromises()

    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0

    const cancelBtn = document.querySelector(
      '[data-testid="rich-note-property-value-popup-cancel"]'
    ) as HTMLButtonElement
    cancelBtn.click()
    await flushPromises()

    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )
    expect(document.querySelector("dialog")).toBeNull()

    const valField = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    expect(valField.text()).toContain("training")
  })

  it("hides list mode for scalar-only structural keys", async () => {
    const markdown = `---
image_mask: region-a
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    expect(
      document.querySelector(
        '[data-testid="rich-note-property-value-popup-mode-list"]'
      )
    ).toBeNull()
    expect(
      document.querySelector(
        '[data-testid="rich-note-property-value-popup-mode-text"]'
      )
    ).not.toBeNull()
  })

  it("shows value edit icon on list property rows", async () => {
    const markdown = `---
tags:
  - alpha
---

Body`
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    expect(
      wrapper
        .find('[data-testid="rich-note-property-value-popup-open"]')
        .exists()
    ).toBe(true)
  })

  it("does not show value edit icon on specialized scalar property rows", async () => {
    const markdown = `---
relation: related-to
wikidata_id: Q42
---

Body`
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    expect(
      wrapper.findAll('[data-testid="rich-note-property-value-popup-open"]')
        .length
    ).toBe(0)
  })
})
