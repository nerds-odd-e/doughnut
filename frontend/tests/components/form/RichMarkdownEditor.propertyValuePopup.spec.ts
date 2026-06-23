import { flushPromises } from "@vue/test-utils"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor scalar property value popup", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  async function openValuePopup(
    wrapper: Awaited<ReturnType<typeof h.mountEditor>>
  ) {
    const openBtn = wrapper.find(
      '[data-testid="rich-note-property-value-popup-open"]'
    )
    expect(openBtn.exists()).toBe(true)
    await openBtn.trigger("click")
    await flushPromises()
  }

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

    const textarea = document.querySelector(
      '[data-testid="rich-note-property-value-popup-textarea"]'
    ) as HTMLTextAreaElement
    textarea.value = "advanced workshop"
    textarea.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()

    const saveBtn = document.querySelector(
      '[data-testid="rich-note-property-value-popup-save"]'
    ) as HTMLButtonElement
    saveBtn.click()
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

    const textarea = document.querySelector(
      '[data-testid="rich-note-property-value-popup-textarea"]'
    ) as HTMLTextAreaElement
    textarea.value = "changed but not saved"
    textarea.dispatchEvent(new Event("input", { bubbles: true }))
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

  it("does not show value edit icon on list property rows", async () => {
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
    ).toBe(false)
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
