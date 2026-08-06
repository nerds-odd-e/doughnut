import { flushPromises } from "@vue/test-utils"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const listUrlMarkdown = `---
url:
  - https://example.com/a
  - https://example.com/b
---

# Body`

describe("RichMarkdownEditor list properties", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
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
    const rows = wrapper.findAll('[data-testid="rich-note-property-row"]')
    expect(rows.length).toBe(2)
    const tagsRow = rows.find(
      (r) => (r.element as HTMLElement).dataset.propertyKey === "tags"
    )
    const exampleRow = rows.find(
      (r) => (r.element as HTMLElement).dataset.propertyKey === "example of"
    )
    expect(
      tagsRow!.find('[data-testid="rich-note-property-row-list-value"]').text()
    ).toContain("alpha")
    expect(
      exampleRow!
        .find('[data-testid="rich-note-property-row-list-value"]')
        .text()
    ).toContain("one")
    expect(
      tagsRow!
        .find('[data-testid="rich-note-property-value-popup-open"]')
        .exists()
    ).toBe(true)
  })

  it("shows per-item external links for list url in readonly mode", async () => {
    const wrapper = await h.mountEditor(listUrlMarkdown, { readonly: true })
    const links = wrapper
      .find("dl")
      .findAll('[data-testid="rich-note-property-external-link"]')
    expect(links.length).toBe(2)
    expect(wrapper.find("dl").text()).toContain("https://example.com/a")
  })

  it("shows per-item external links for list url in editable mode", async () => {
    const wrapper = await h.mountEditor(listUrlMarkdown)
    await flushPromises()

    const urlRow = wrapper
      .findAll('[data-testid="rich-note-property-row"]')
      .find((r) => (r.element as HTMLElement).dataset.propertyKey === "url")
    expect(
      urlRow!.findAll('[data-testid="rich-note-property-external-link"]').length
    ).toBe(2)
  })

  it("shows list properties compactly in readonly mode", async () => {
    const markdown = `---
tags:
  - alpha
  - beta
---

# Body`
    const wrapper = await h.mountEditor(markdown, { readonly: true })
    expect(wrapper.find("dl").text()).toContain("alpha")
  })

  it("preserves list frontmatter when body is edited", async () => {
    const markdown = `---
tags:
  - alpha
  - beta
---

# Original`
    await h.mountEditor(markdown)
    h.emitQuillModelValue("<h1>Edited Heading</h1>")

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("- alpha")
    expect(last).toContain("Edited Heading")
  })

  it("preserves list frontmatter on pasteComplete", async () => {
    const markdown = `---
tags:
  - a1
  - a2
---

Hello`
    await h.mountEditor(markdown)
    h.emitQuillPasteComplete("<p>Pasted</p>")

    const payload = h.lastEmittedPasteComplete()
    expect(payload).toContain("- a1")
    expect(payload).toMatch(/^---\n/)
  })
})
