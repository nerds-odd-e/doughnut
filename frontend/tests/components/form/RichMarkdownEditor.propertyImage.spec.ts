import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import { vi } from "vitest"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor image property value and upload", () => {
  const h = createRichMarkdownEditorTestHarness()
  let uploadSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    uploadSpy = mockSdkService(NoteController, "uploadNoteImage", {
      imagePath: "/attachments/images/99/e2e.png",
    })
  })

  afterEach(() => {
    h.cleanup()
    vi.restoreAllMocks()
  })

  it("adds an image property from a typed URL without requiring an upload", async () => {
    const wrapper = await h.mountEditor("# Hi")

    await h.openAddProperty()
    await wrapper
      .find('[data-testid="rich-note-property-key"]')
      .setValue("image")
    await flushPromises()

    const valInput = wrapper.find('[data-testid="rich-note-property-value"]')
    await valInput.setValue("https://example.com/a.png")
    await valInput.trigger("blur")

    expect(uploadSpy).not.toHaveBeenCalled()
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

    expect(uploadSpy).not.toHaveBeenCalled()
    expect(h.lastEmittedMarkdown()).toContain(
      "image: https://example.com/new.png"
    )
  })

  it("sets image path from upload when choosing a file on the image row", async () => {
    const markdown = `---
image: /attachments/images/1/old.png
---

# Hi`
    const wrapper = await h.mountEditor(markdown, { noteId: 42 })

    const fileInput = wrapper.find(
      '[data-testid="rich-note-image-property-file-input"]'
    )
    const inputEl = fileInput.element as HTMLInputElement
    const file = new File(["fake"], "pic.png", { type: "image/png" })
    Object.defineProperty(inputEl, "files", {
      value: [file],
      configurable: true,
    })
    await fileInput.trigger("change")

    expect(uploadSpy).toHaveBeenCalled()
    expect(h.lastEmittedMarkdown()).toContain(
      "image: /attachments/images/99/e2e.png"
    )
  })
})
