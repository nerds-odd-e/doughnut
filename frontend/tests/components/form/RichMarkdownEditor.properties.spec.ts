import { flushPromises } from "@vue/test-utils"
import { noteShowLocation } from "@/routes/noteShowLocation"
import {
  expandPropertyPanel,
  expectPropertyPanelClosed,
  expectPropertyPanelOpen,
  propertyRowKeyInputEl,
  propertyRowSelector,
  propertyRows,
  propertyValidationText,
} from "./propertiesTestDom"
import {
  attemptRenamePropertyKey,
  mountDuplicateKeysEditor,
} from "./propertiesTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const twoPropertyMarkdown = `---
alpha: one
beta: two
---

Body line`

describe("RichMarkdownEditor properties", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("shows read-only Properties above Quill when content includes supported YAML frontmatter", async () => {
    const markdown = `---
diligence: high
topic: training
---

# Workshop Body

Main content here.`
    const wrapper = await h.mountEditor(markdown, { readonly: true })

    expect(wrapper.find("h4").text()).toBe("Properties")
    const readOnlyList = wrapper.find("dl")
    expect(readOnlyList.text()).toContain("diligence")
    expect(readOnlyList.text()).toContain("training")

    const html = h.quillModelHtml()
    expect(html).toContain("Workshop Body")
    expect(html).not.toContain("diligence:")
  })

  it("shows add-only chrome when content has no or empty frontmatter, hides section when readonly", async () => {
    for (const md of [
      "# Hello\n\nParagraph.",
      `---


---

Body`,
    ]) {
      let wrapper = await h.mountEditor(md)
      expect(wrapper.find("h4").exists()).toBe(false)
      expect(wrapper.text()).toContain("Add property")

      wrapper = await h.mountEditor(md, { readonly: true })
      expect(wrapper.find("section").exists()).toBe(false)
    }
  })

  it("invalid YAML: hides Properties, shows alert, freezes Quill, ignores body edits", async () => {
    const markdown = `---
bad:
  nested: value
---

Still body`
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    expect(wrapper.find("section").exists()).toBe(false)
    const alert = wrapper.find(
      '[data-testid="rich-note-frontmatter-parse-error"]'
    )
    expect(alert.text()).toContain("string")
    expect(alert.text()).toContain("Markdown mode")

    expect(h.quillReadonly()).toBe(true)
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0
    h.emitQuillModelValue("<p>Edited without fixing YAML</p>")
    await flushPromises()
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )
  })

  it("composes edited body with existing frontmatter when emitting updates", async () => {
    const markdown = `---
diligence: high
topic: training
---

# Original`
    await h.mountEditor(markdown)
    h.emitQuillModelValue("<h1>Edited Heading</h1>")

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("diligence:")
    expect(last).toContain("Edited Heading")
  })

  it("emits pasteComplete with full composed markdown so link-removal preserves frontmatter", async () => {
    const markdown = `---
topic: training
---

Hello`
    await h.mountEditor(markdown)
    h.emitQuillPasteComplete(
      '<p>Hello <a href="https://example.com" rel="noopener noreferrer" target="_blank">x</a></p>'
    )

    const payload = h.lastEmittedPasteComplete()
    expect(payload).toContain("topic: training")
    expect(payload).toMatch(/^---\n/)
  })

  it("editing an existing property row emits renamed keys and updated values", async () => {
    const markdown = `---
topic: training
---

Workshop body.`
    const wrapper = await h.mountEditor(markdown)

    const keyInput = wrapper.find(
      '[data-testid="rich-note-property-row-key-input"]'
    )
    const valInput = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    await keyInput.setValue("domain")
    await keyInput.trigger("blur")
    await h.setPropertyValueField(valInput, "wiki")
    await valInput.trigger("blur")

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("domain:")
    expect(last).toContain("wiki")
    expect(last).not.toContain("topic:")
  })

  it("shows validation and does not emit corrupt duplicate keys when renaming a row", async () => {
    const wrapper = await mountDuplicateKeysEditor(h)
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0

    await attemptRenamePropertyKey(wrapper, 1, "alpha")

    expect(propertyValidationText(wrapper.element)).toContain("Duplicate")
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )
    expect(propertyRowKeyInputEl(propertyRows(wrapper.element)[1]!).value).toBe(
      "beta"
    )
  })

  it("opening one property panel then removing that row leaves the other collapsed", async () => {
    const wrapper = await h.mountEditor(twoPropertyMarkdown, {
      route: noteShowLocation(42),
    })
    const alphaRow = propertyRowSelector("alpha")
    const betaRow = propertyRowSelector("beta")
    const rows = propertyRows(wrapper.element)

    expectPropertyPanelClosed(rows[0]!)
    expectPropertyPanelClosed(rows[1]!)

    await expandPropertyPanel(wrapper, alphaRow)

    expectPropertyPanelOpen(wrapper.find(alphaRow).element)
    expectPropertyPanelClosed(wrapper.find(betaRow).element)

    await wrapper
      .find(`${alphaRow} [data-testid="rich-note-property-row-remove"]`)
      .trigger("click")
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).not.toContain("alpha:")
    expect(last).toContain("beta:")

    expectPropertyPanelClosed(wrapper.find(betaRow).element)
  })
})
