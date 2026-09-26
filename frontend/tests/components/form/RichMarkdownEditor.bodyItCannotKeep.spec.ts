import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor with a body it cannot keep", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  const warningText = () =>
    h.getWrapper().find('[data-testid="rich-note-unavailable-warning"]').text()

  it.each([
    "<details><summary>S</summary>Inner</details>",
    "- [ ] todo\n- [x] done",
    "line one  \nline two",
    "**a** *b*",
    "```ts\nconst a = 1\n```\n\n```\nplain\n```",
  ])("opens read-only with the warning: %j", async (body) => {
    await h.mountEditor(body)

    expect(h.quillReadonly()).toBe(true)
    expect(warningText()).toContain("rich editor cannot keep")
    expect(warningText()).toContain("Switch to Markdown mode")
  })

  it.each([
    "# Heading\n\n- one\n- two\n\nSee [the site](https://example.com).",
    "Heading\n=======\n\n* one\n* two\n\nSee [the site][site].\n\n[site]: https://example.com",
    "See [[Some Note]] and [[Other|alias]].",
    "line one\nline two",
    "- item that wraps\n  onto a second line\n- two",
  ])("keeps a body with only style differences editable: %j", async (body) => {
    await h.mountEditor(body)

    expect(h.quillReadonly()).toBe(false)
    h.quillInstance().insertText(0, "Typed ", "user")
    expect(h.lastEmittedMarkdown()).toContain("Typed")
  })
})
