import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"

function createClipboardEvent(html: string): ClipboardEvent {
  const event = new Event("paste", {
    bubbles: true,
    cancelable: true,
  }) as ClipboardEvent
  const dataTransfer = {
    getData: (format: string) => {
      if (format === "text/html") return html
      return ""
    },
    setData: () => {
      // Mock implementation - not used in tests
    },
  }
  Object.defineProperty(event, "clipboardData", {
    value: dataTransfer,
    writable: false,
  })
  return event
}

describe("RichMarkdownEditor", () => {
  const mountEditor = async (initialValue: string, options = {}) => {
    const wrapper = helper
      .component(RichMarkdownEditor)
      .withProps({
        modelValue: initialValue,
        ...options,
      })
      .mount()
    await flushPromises()
    return wrapper
  }

  it("not emit update when the change is from initial value", async () => {
    const wrapper = await mountEditor("initial value")
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("not emit update if readonly", async () => {
    const wrapper = await mountEditor("# Title", { readonly: true })
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("will try to unify the markdown", async () => {
    const wrapper = await mountEditor("# Title")
    await flushPromises()
    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted![0]![0]).toEqual("Title\n=====")
  })

  it("converts HTML to markdown then back to HTML when pasting", async () => {
    const wrapper = await mountEditor("")
    await flushPromises()

    const quillEditor = wrapper.findComponent({ name: "QuillEditor" })
    const quillElement = quillEditor.vm.$el as HTMLElement
    // Find the .ql-editor element where the paste listener is attached
    const qlEditor = quillElement.querySelector(".ql-editor") as HTMLElement
    expect(qlEditor).toBeTruthy()

    // Create a paste event with HTML content
    const pasteEvent = createClipboardEvent("<p><strong>Bold text</strong></p>")

    // Trigger paste event on the .ql-editor element
    qlEditor.dispatchEvent(pasteEvent)
    await flushPromises()

    // The HTML should be converted to markdown, then back to HTML
    // So we should see the markdown emitted
    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted).toBeDefined()
    // The markdown should contain "Bold text" or "**Bold text**"
    const lastEmitted = emitted![emitted!.length - 1]![0] as string
    expect(lastEmitted).toContain("Bold text")
  })

  it("does not handle paste when readonly", async () => {
    const wrapper = await mountEditor("", { readonly: true })
    await flushPromises()

    const quillEditor = wrapper.findComponent({ name: "QuillEditor" })
    const quillElement = quillEditor.vm.$el as HTMLElement
    const qlEditor = quillElement.querySelector(".ql-editor") as HTMLElement
    expect(qlEditor).toBeTruthy()

    const pasteEvent = createClipboardEvent("<p>Test</p>")

    qlEditor.dispatchEvent(pasteEvent)
    await flushPromises()

    // Should not emit update when readonly
    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted).toBeUndefined()
  })
})
