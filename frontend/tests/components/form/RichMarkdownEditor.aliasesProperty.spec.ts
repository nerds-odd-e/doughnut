import { flushPromises } from "@vue/test-utils"
import { AUTHORED_ALIASES_MESSAGE } from "@/utils/authoredAliasesValidation"
import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import {
  clickListAdd,
  clickModeTab,
  clickSave,
  openValuePopup,
  setListItemValue,
  setTextareaValue,
} from "./propertyValuePopupTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor aliases property", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("shows alias constraint when aliases is saved as scalar text in popup", async () => {
    const markdown = `---
aliases:
  - color
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    clickModeTab("rich-note-property-value-popup-mode-text")
    await flushPromises()
    setTextareaValue("single alias")
    clickSave()
    await flushPromises()

    expect(
      document.querySelector(
        '[data-testid="rich-note-property-value-popup-validation"]'
      )?.textContent
    ).toBe(AUTHORED_ALIASES_MESSAGE)
    expect(document.querySelector("dialog")).not.toBeNull()
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()
  })

  it("shows alias constraint for invalid list items in popup", async () => {
    const markdown = `---
aliases:
  - color
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    setListItemValue(0, "bad|alias")
    clickSave()
    await flushPromises()

    expect(
      document.querySelector(
        '[data-testid="rich-note-property-value-popup-validation"]'
      )?.textContent
    ).toBe(AUTHORED_ALIASES_MESSAGE)
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()
  })

  it("emits valid aliases list edits from popup", async () => {
    const markdown = `---
aliases:
  - color
---

Body`
    const wrapper = await h.mountEditor(markdown, { attachToBody: true })
    await openValuePopup(wrapper)

    clickListAdd()
    await flushPromises()
    setListItemValue(1, "hue")
    clickSave()
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toMatch(/aliases:\s*\n\s*- color/)
    expect(last).toMatch(/- hue/)
    expect(document.querySelector("dialog")).toBeNull()
  })

  it("inserts the first alias as a list when adding a new aliases property", async () => {
    await h.mountEditor("# Body", { attachToBody: true })
    await h.openAddProperty()

    const keyInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-key"]')
    const valInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-value"]')
    await keyInput.setValue("aliases")
    await h.setWikiPropertyValueField(valInput, "color")
    await valInput.trigger("blur")
    await flushPromises()

    expect(
      h
        .getWrapper()
        .find('[data-testid="rich-note-property-validation"]')
        .exists()
    ).toBe(false)
    const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties.aliases).toEqual(listPropertyValue(["color"]))
  })

  it("blocks commit when parsed aliases row is scalar", async () => {
    const markdown = `---
aliases: color
---

Body`
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    const keyInput = wrapper.find(
      '[data-testid="rich-note-property-row-key-input"]'
    )
    await keyInput.trigger("focus")
    await keyInput.trigger("blur")
    await flushPromises()

    expect(
      wrapper.find('[data-testid="rich-note-property-validation"]').text()
    ).toBe(AUTHORED_ALIASES_MESSAGE)
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()
  })
})
