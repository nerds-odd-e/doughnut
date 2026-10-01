import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor image property editing", () => {
  const h = createRichMarkdownEditorTestHarness()
  afterEach(() => {
    h.cleanup()
  })

  it("updates an existing image property from typed text", async () => {
    const wrapper = await h.mountEditor(
      "---\nimage: https://example.com/old.png\n---\n\n# Hi"
    )

    const valInput = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    await valInput.setValue("https://example.com/new.png")
    await valInput.trigger("blur")

    expect(h.lastEmittedMarkdown()).toContain(
      "image: https://example.com/new.png"
    )
  })
})
