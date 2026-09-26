import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor frontmatter around the body", () => {
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

  it.each([
    "bad: [",
    "author: first\nauthor: second",
    "custom:\n  source: first\n  source: second",
    "- item",
    "custom:\n  source: local\ntype: [Note]",
    "custom:\n  source: local\nsource:\n  path: target",
    "custom: null",
  ])("protects invalid frontmatter: %s", async (yaml) => {
    const markdown = `---
${yaml}
---

Still body`
    const wrapper = await h.mountEditor(markdown)

    expect(wrapper.find("section").exists()).toBe(false)
    const alert = wrapper.find('[data-testid="rich-note-unavailable-warning"]')
    expect(alert.text()).toContain("Markdown mode")

    expect(h.quillReadonly()).toBe(true)
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0
    h.emitQuillModelValue("<p>Edited without fixing YAML</p>")
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )
  })

  it.each([
    "custom:\n  source: 'local'",
    "custom:\n  - source: 'local'",
    "custom: [[local]]",
  ])(
    "renders valid nested metadata separately from the body: %s",
    async (metadata) => {
      const wrapper = await h.mountEditor(
        `---\ntype: Note\n# Author annotation\n${metadata}\n---\nOriginal body.`
      )

      expect(h.quillEditorEl().textContent).toBe("Original body.")
      expect(wrapper.text()).toContain("Edit metadata in Markdown.")
      expect(wrapper.find('[role="alert"]').exists()).toBe(false)
      expect(wrapper.find("section").exists()).toBe(false)
      expect(h.quillReadonly()).toBe(false)
    }
  )

  it.each([
    {
      kind: "scalar",
      frontmatter: "diligence: high\nimage: force-diagram.png",
      kept: ["diligence:", "image: force-diagram.png"],
    },
    {
      kind: "list",
      frontmatter: "tags:\n  - alpha\n  - beta",
      kept: ["- alpha"],
    },
  ])(
    "composes $kind frontmatter with the edited body and with pasteComplete",
    async ({ frontmatter, kept }) => {
      await h.mountEditor(`---\n${frontmatter}\n---\n\n# Original`)

      h.emitQuillModelValue("<h1>Edited Heading</h1>")
      const last = h.lastEmittedMarkdown()
      for (const text of kept) expect(last).toContain(text)
      expect(last).toContain("Edited Heading")

      h.emitQuillPasteComplete(
        '<p>Hello <a href="https://example.com" rel="noopener noreferrer" target="_blank">x</a></p>'
      )
      const payload = h.lastEmittedPasteComplete()
      for (const text of kept) expect(payload).toContain(text)
      expect(payload).toMatch(/^---\n/)
    }
  )

  describe("nested metadata", () => {
    it.each(["\n", "\r\n"])(
      "preserves the authored %j prefix for typing and paste",
      async (newline) => {
        const prefix = [
          "---",
          "type: Note",
          "# Author annotation",
          "custom:",
          "  source: 'local'",
          "  tags: [one, two]",
          'author: "Ada"',
          "---",
          "",
        ].join(newline)
        const wrapper = await h.mountEditor(`${prefix}Original body.`)
        const html = "<p><strong>Edited body.</strong></p>"
        h.emitQuillModelValue(html)
        expect(h.lastEmittedMarkdown()).toBe(`${prefix}**Edited body.**`)
        await wrapper.setProps({ modelValue: h.lastEmittedMarkdown() })
        expect(h.quillModelHtml()).toBe(html)
        h.emitQuillPasteComplete("<p>Pasted body.</p>")
        expect(h.lastEmittedPasteComplete()).toBe(`${prefix}Pasted body.`)
      }
    )

    it.each(["\n", "\r\n"])(
      "separates a new body from an unterminated %j closing fence",
      async (newline) => {
        const prefix = ["---", "custom:", "  source: local", "---"].join(
          newline
        )
        await h.mountEditor(prefix)
        h.emitQuillModelValue("<p>New body.</p>")
        expect(h.lastEmittedMarkdown()).toBe(`${prefix}${newline}New body.`)
        h.emitQuillPasteComplete("<p>Pasted body.</p>")
        expect(h.lastEmittedPasteComplete()).toBe(
          `${prefix}${newline}Pasted body.`
        )
      }
    )

    it("keeps an explicitly readonly nested note locked", async () => {
      await h.mountEditor("---\ncustom: {source: local}\n---\nBody", {
        readonly: true,
      })
      expect(h.quillReadonly()).toBe(true)
      await h.dispatchPasteHtmlToQuill("<p>Pasted body.</p>")
      expect(h.getWrapper().emitted("update:modelValue")).toBeUndefined()
    })

    it("keeps the body locked during an image upload", async () => {
      const wrapper = await h.mountEditor(
        "---\ncustom: {source: local}\n---\nBody"
      )
      wrapper
        .findComponent({ name: "RichFrontmatterProperties" })
        .vm.$emit("image-upload-state", true)
      await wrapper.vm.$nextTick()
      expect(h.quillReadonly()).toBe(true)
      await h.dispatchPasteHtmlToQuill("<p>Pasted body.</p>")
      expect(wrapper.emitted("update:modelValue")).toBeUndefined()
    })
  })
})
