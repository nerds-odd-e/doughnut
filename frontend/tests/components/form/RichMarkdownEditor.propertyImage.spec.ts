import { flushPromises } from "@vue/test-utils"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor image property value", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("adds an image property from a typed URL", async () => {
    const wrapper = await h.mountEditor("# Hi")

    await h.openAddProperty()
    await wrapper
      .find('[data-testid="rich-note-property-key"]')
      .setValue("image")
    await flushPromises()

    const valInput = wrapper.find('[data-testid="rich-note-property-value"]')
    await valInput.setValue("https://example.com/a.png")
    await valInput.trigger("blur")

    expect(h.lastEmittedMarkdown()).toContain(
      "image: https://example.com/a.png"
    )
  })

  it("updates an existing image property from typed text", async () => {
    const markdown = `---
image: https://example.com/old.png
---

# Hi`
    const wrapper = await h.mountEditor(markdown)

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
