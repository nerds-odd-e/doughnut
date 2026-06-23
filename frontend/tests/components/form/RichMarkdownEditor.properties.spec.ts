import { flushPromises } from "@vue/test-utils"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

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

    const propertiesHeading = wrapper.find("h4")
    expect(propertiesHeading.exists()).toBe(true)
    expect(propertiesHeading.text()).toBe("Properties")
    const readOnlyList = wrapper.find("dl")
    expect(readOnlyList.text()).toContain("diligence")
    expect(readOnlyList.text()).toContain("high")
    expect(readOnlyList.text()).toContain("topic")
    expect(readOnlyList.text()).toContain("training")

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    const html = String(quill.props("modelValue"))
    expect(html).toContain("Workshop Body")
    expect(html).not.toContain("diligence:")
    expect(html).not.toContain("topic:")
  })

  it("shows add-only chrome when content has no or empty frontmatter, hides section when readonly", async () => {
    for (const md of [
      "# Hello\n\nParagraph.",
      `---


---

Body`,
    ]) {
      let wrapper = await h.mountEditor(md)
      expect(wrapper.find("section").exists()).toBe(true)
      expect(wrapper.find("h4").exists()).toBe(false)
      expect(wrapper.text()).not.toContain("Properties")
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
    expect(alert.exists()).toBe(true)
    expect(alert.text()).toContain("string")
    expect(alert.text()).toContain("Markdown mode")

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    expect(quill.props("readonly")).toBe(true)
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0
    quill.vm.$emit("update:modelValue", "<p>Edited without fixing YAML</p>")
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

    const quill = h.getWrapper().findComponent({ name: "QuillEditor" })
    quill.vm.$emit("update:modelValue", "<h1>Edited Heading</h1>")

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("diligence:")
    expect(last).toContain("topic:")
    expect(last).toContain("Edited Heading")
  })

  it("emits pasteComplete with full composed markdown so link-removal preserves frontmatter", async () => {
    const markdown = `---
topic: training
---

Hello`
    const wrapper = await h.mountEditor(markdown)

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    quill.vm.$emit(
      "pasteComplete",
      '<p>Hello <a href="https://example.com" rel="noopener noreferrer" target="_blank">x</a></p>'
    )

    const emitted = wrapper.emitted("pasteComplete")
    expect(emitted?.length).toBeGreaterThan(0)
    const payload = emitted![emitted!.length - 1]![0] as string
    expect(payload).toContain("topic: training")
    expect(payload).toMatch(/^---\n/)
  })

  it("emits deadLinkClick when a dead wiki link in a property value is clicked", async () => {
    const markdown = `---
topic: "[[Missing Note]]"
---

Body`
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    const valField = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    const dead = valField.find("a.dead-link")
    expect(dead.exists()).toBe(true)
    await dead.trigger("click")
    await flushPromises()
    expect(wrapper.emitted("deadLinkClick")?.[0]).toEqual([
      { targetToken: "Missing Note", displayText: "Missing Note" },
    ])
  })

  it("emits deadLinkClick with target token when a property wiki link uses display text", async () => {
    const markdown = `---
topic: "[[Ghost Page|shown text]]"
---

Body`
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    const valField = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    const dead = valField.find("a.dead-link")
    expect(dead.exists()).toBe(true)
    await dead.trigger("click")
    await flushPromises()
    expect(wrapper.emitted("deadLinkClick")?.[0]).toEqual([
      { targetToken: "Ghost Page", displayText: "shown text" },
    ])
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
    await h.setWikiPropertyValueField(valInput, "wiki")
    await valInput.trigger("blur")

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("domain:")
    expect(last).toContain("wiki")
    expect(last).not.toContain("topic:")
    expect(last).toContain("Workshop body")
  })

  it("shows validation and does not emit corrupt duplicate keys when renaming a row", async () => {
    const markdown = `---
alpha: one
beta: two
---

Body`
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    const rows = wrapper.findAll('[data-testid="rich-note-property-row"]')
    expect(rows.length).toBe(2)
    const betaKeyInput = rows[1]!.find(
      '[data-testid="rich-note-property-row-key-input"]'
    )
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0

    await betaKeyInput.trigger("focus")
    await betaKeyInput.setValue("alpha")
    await betaKeyInput.trigger("blur")
    await flushPromises()

    expect(
      wrapper.find('[data-testid="rich-note-property-validation"]').text()
    ).toContain("Duplicate")

    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )

    const betaKeyAfter = wrapper
      .findAll('[data-testid="rich-note-property-row"]')[1]!
      .find('[data-testid="rich-note-property-row-key-input"]')
    expect((betaKeyAfter.element as HTMLInputElement).value).toBe("beta")
  })

  it("removing one property row emits markdown without that key and retains the rest", async () => {
    const markdown = `---
alpha: one
beta: two
---

Body line`
    const wrapper = await h.mountEditor(markdown)

    const removeBtns = wrapper.findAll(
      '[data-testid="rich-note-property-row-remove"]'
    )
    expect(removeBtns.length).toBe(2)
    await removeBtns[0]!.trigger("click")

    const last = h.lastEmittedMarkdown()
    expect(last).not.toContain("alpha:")
    expect(last).toContain("beta:")
    expect(last).toContain("Body line")
  })

  it("shows imported list properties without parse error banner", async () => {
    const markdown = `---
tags:
  - alpha
  - beta
example of:
  - one
  - two
---

# Body`
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    expect(
      wrapper.find('[data-testid="rich-note-frontmatter-parse-error"]').exists()
    ).toBe(false)
    expect(wrapper.find("h4").text()).toBe("Properties")
    const rows = wrapper.findAll('[data-testid="rich-note-property-row"]')
    expect(rows.length).toBe(2)
    const tagsRow = rows.find(
      (r) => (r.element as HTMLElement).dataset.propertyKey === "tags"
    )
    const exampleRow = rows.find(
      (r) => (r.element as HTMLElement).dataset.propertyKey === "example of"
    )
    expect(tagsRow).toBeDefined()
    expect(exampleRow).toBeDefined()
    expect(
      tagsRow!.find('[data-testid="rich-note-property-row-list-value"]').text()
    ).toContain("alpha")
    expect(
      tagsRow!.find('[data-testid="rich-note-property-row-list-value"]').text()
    ).toContain("beta")
    expect(
      exampleRow!
        .find('[data-testid="rich-note-property-row-list-value"]')
        .text()
    ).toContain("one")
    expect(
      exampleRow!
        .find('[data-testid="rich-note-property-row-list-value"]')
        .text()
    ).toContain("two")
    expect(
      tagsRow!
        .find('[data-testid="rich-note-property-row-list-value"]')
        .exists()
    ).toBe(true)
    expect(
      tagsRow!
        .find('[data-testid="rich-note-property-value-popup-open"]')
        .exists()
    ).toBe(true)
  })

  it("shows per-item external links for list url in readonly mode", async () => {
    const markdown = `---
url:
  - https://example.com/a
  - https://example.com/b
---

# Body`
    const wrapper = await h.mountEditor(markdown, { readonly: true })
    const readOnlyList = wrapper.find("dl")
    expect(readOnlyList.text()).toContain("https://example.com/a")
    expect(readOnlyList.text()).toContain("https://example.com/b")
    const links = readOnlyList.findAll(
      '[data-testid="rich-note-property-external-link"]'
    )
    expect(links.length).toBe(2)
  })

  it("shows per-item external links for list url in editable mode", async () => {
    const markdown = `---
url:
  - https://example.com/a
  - https://example.com/b
---

# Body`
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    const urlRow = wrapper
      .findAll('[data-testid="rich-note-property-row"]')
      .find((r) => (r.element as HTMLElement).dataset.propertyKey === "url")
    expect(urlRow).toBeDefined()
    expect(
      urlRow!.find('[data-testid="rich-note-property-row-list-value"]').text()
    ).toContain("https://example.com/a")
    expect(
      urlRow!.find('[data-testid="rich-note-property-row-list-value"]').text()
    ).toContain("https://example.com/b")
    const links = urlRow!.findAll(
      '[data-testid="rich-note-property-external-link"]'
    )
    expect(links.length).toBe(2)
  })

  it("shows list properties compactly in readonly mode", async () => {
    const markdown = `---
tags:
  - alpha
  - beta
---

# Body`
    const wrapper = await h.mountEditor(markdown, { readonly: true })
    const readOnlyList = wrapper.find("dl")
    expect(readOnlyList.text()).toContain("tags")
    expect(readOnlyList.text()).toContain("alpha")
    expect(readOnlyList.text()).toContain("beta")
  })

  it("preserves list frontmatter when body is edited", async () => {
    const markdown = `---
tags:
  - alpha
  - beta
---

# Original`
    await h.mountEditor(markdown)

    const quill = h.getWrapper().findComponent({ name: "QuillEditor" })
    quill.vm.$emit("update:modelValue", "<h1>Edited Heading</h1>")

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("tags:")
    expect(last).toContain("- alpha")
    expect(last).toContain("- beta")
    expect(last).toContain("Edited Heading")
  })

  it("preserves list frontmatter on pasteComplete", async () => {
    const markdown = `---
tags:
  - a1
  - a2
---

Hello`
    const wrapper = await h.mountEditor(markdown)

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    quill.vm.$emit("pasteComplete", "<p>Pasted</p>")

    const emitted = wrapper.emitted("pasteComplete")
    expect(emitted?.length).toBeGreaterThan(0)
    const payload = emitted![emitted!.length - 1]![0] as string
    expect(payload).toContain("tags:")
    expect(payload).toContain("- a1")
    expect(payload).toMatch(/^---\n/)
  })
})
