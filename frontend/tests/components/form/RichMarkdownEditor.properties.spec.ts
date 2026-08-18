import { flushPromises } from "@vue/test-utils"
import {
  expandAndClickPropertyRowRemove,
  expandPropertyRowOptions,
  propertyRowKeyInputEl,
  propertyRowOptionsPanelEl,
  propertyRowOptionsToggleEl,
  propertyRowSelector,
  propertyRows,
  propertyValidationText,
} from "./propertiesTestDom"
import {
  attemptRenamePropertyKey,
  clickDeadWikiLinkInPropertyValue,
  DEAD_LINK_CLICK_CASES,
  mountDuplicateKeysEditor,
  propertyWikiLinkMarkdown,
} from "./propertiesTestSupport"
import { relationshipNoteContent } from "@tests/notes/relationshipNoteTestContent"
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

  it.each(DEAD_LINK_CLICK_CASES)(
    "emits deadWikiLinkClick for property wiki link ($case)",
    async ({ wikiToken, expected }) => {
      const wrapper = await h.mountEditor(propertyWikiLinkMarkdown(wikiToken))
      await clickDeadWikiLinkInPropertyValue(wrapper)
      expect(wrapper.emitted("deadWikiLinkClick")?.[0]).toEqual([expected])
    }
  )

  it("shows path Markdown in a relationship source as a live wiki-style link", async () => {
    const wrapper = await h.mountEditor(
      relationshipNoteContent("a-part-of", "[Moon](/Moon.md)", "[[Earth]]"),
      {
        wikiTitles: [
          {
            linkText: "[Moon](/Moon.md)",
            targetToken: "/Moon.md",
            displayText: "Moon",
            noteId: 42,
          },
        ],
      }
    )
    const live = wrapper
      .find(propertyRowSelector("source"))
      .element.querySelector("a.doughnut-wiki-link") as HTMLAnchorElement
    expect(live.getAttribute("href")).toBe("/Moon.md")
    expect(live.textContent).toBe("Moon")
    expect(live.querySelector(".wiki-bracket")).toBeNull()
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

  it("removing one property row emits markdown without that key and retains the rest", async () => {
    const wrapper = await h.mountEditor(twoPropertyMarkdown)

    await expandAndClickPropertyRowRemove(wrapper, propertyRowSelector("alpha"))

    const last = h.lastEmittedMarkdown()
    expect(last).not.toContain("alpha:")
    expect(last).toContain("beta:")
  })

  it("removing an expanded property leaves remaining rows collapsed", async () => {
    const wrapper = await h.mountEditor(twoPropertyMarkdown)

    await expandAndClickPropertyRowRemove(wrapper, propertyRowSelector("alpha"))

    const betaRow = wrapper.find(propertyRowSelector("beta")).element
    expect(propertyRowOptionsPanelEl(betaRow)).toBeNull()
    expect(
      propertyRowOptionsToggleEl(betaRow).getAttribute("aria-expanded")
    ).toBe("false")
  })

  it("caret toggles options panel and rows expand independently", async () => {
    const wrapper = await h.mountEditor(twoPropertyMarkdown)
    const rows = propertyRows(wrapper.element)

    const alphaRow = propertyRowSelector("alpha")
    const betaRow = propertyRowSelector("beta")

    expect(propertyRowOptionsPanelEl(rows[0]!)).toBeNull()
    expect(propertyRowOptionsPanelEl(rows[1]!)).toBeNull()

    await expandPropertyRowOptions(wrapper, alphaRow)

    expect(
      propertyRowOptionsPanelEl(wrapper.find(alphaRow).element)
    ).not.toBeNull()
    expect(propertyRowOptionsPanelEl(wrapper.find(betaRow).element)).toBeNull()

    expect(
      propertyRowOptionsToggleEl(rows[0]!).getAttribute("aria-expanded")
    ).toBe("true")
    expect(
      propertyRowOptionsToggleEl(rows[1]!).getAttribute("aria-expanded")
    ).toBe("false")
  })
})
