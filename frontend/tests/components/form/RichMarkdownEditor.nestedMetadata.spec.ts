import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor nested metadata body editing", () => {
  const h = createRichMarkdownEditorTestHarness()
  afterEach(() => h.cleanup())

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
      const prefix = ["---", "custom:", "  source: local", "---"].join(newline)
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
