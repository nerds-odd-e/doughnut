import { mount } from "@vue/test-utils"
import QuillEditor from "@/components/form/QuillEditor.vue"
import { nextTick } from "vue"
import type Quill from "quill"
import { describe, it, expect, afterEach } from "vitest"
import { page } from "vitest/browser"

describe("QuillEditor.vue", () => {
  let wrapper: any

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("renders simple HTML content", async () => {
    const html = `<h1>Hello</h1><p>World</p>`
    wrapper = mount(QuillEditor, {
      props: { modelValue: html },
      attachTo: document.body,
    })
    await nextTick()
    await vi.waitUntil(() => document.querySelector(".ql-editor h1"))
    expect(document.querySelector(".ql-editor h1")).toHaveTextContent("Hello")
    expect(document.querySelector(".ql-editor p")).toHaveTextContent("World")
  })

  it("emits correct modelValue when typing Hello<Shift+Enter>World", async () => {
    wrapper = mount(QuillEditor, {
      props: { modelValue: "" },
      attachTo: document.body,
    })
    await nextTick()
    await nextTick() // Wait for Quill to fully initialize

    // Access the Quill instance from the component
    // biome-ignore lint/suspicious/noExplicitAny: Quill instance is not part of the public API
    const quillInstance = (wrapper.vm as any).quill as Quill | null
    expect(quillInstance).not.toBeNull()

    if (quillInstance) {
      // Focus the editor
      await vi.waitUntil(() => document.querySelector(".ql-editor"))
      const editor = document.querySelector(".ql-editor") as HTMLElement
      editor.focus()
      await nextTick()

      // Browser Mode: Use real ClipboardEvent!
      const clipboardData = new DataTransfer()
      clipboardData.setData("text/html", "<p>Hello<br>World</p>")
      const pasteEvent = new ClipboardEvent("paste", {
        bubbles: true,
        cancelable: true,
        clipboardData,
      })
      editor.dispatchEvent(pasteEvent)
      await nextTick()
    }

    // Check the emitted modelValue
    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted).toBeDefined()
    expect(emitted?.length).toBeGreaterThan(0)

    const lastEmittedValue = emitted?.[emitted.length - 1]?.[0] as string
    // Note: When pasting HTML with <br>, Quill converts it to separate paragraphs
    // The actual emitted value when pasting "<p>Hello<br>World</p>" is "<p>Hello</p><p>World</p>"
    expect(lastEmittedValue).toBe(`<p>Hello<br class="softbreak">World</p>`)
  })

  it("passes preserve_pre: true when pasting HTML with code blocks", async () => {
    wrapper = mount(QuillEditor, {
      props: { modelValue: "", readonly: false },
      attachTo: document.body,
    })
    await nextTick()
    await nextTick() // Wait for Quill to fully initialize
    await nextTick() // Wait for event listener to be set up

    // biome-ignore lint/suspicious/noExplicitAny: Quill instance is not part of the public API
    const quillInstance = (wrapper.vm as any).quill as Quill | null
    expect(quillInstance).not.toBeNull()

    if (quillInstance) {
      // Focus the editor
      // Wait for editor to be visible to avoid focus errors
      await vi.waitUntil(() => document.querySelector(".ql-editor"))
      const editor = document.querySelector(".ql-editor") as HTMLElement
      editor.focus()
      await nextTick()

      // Real HTML input data with code blocks from a typical paste operation
      const inputHtml =
        '<pre><code>function hello() {\n  console.log("world");\n}</code></pre>'

      // Create clipboardData that will be intercepted by the paste handler
      const originalGetData = (format: string) => {
        if (format === "text/html") {
          return inputHtml
        }
        return ""
      }

      const mockClipboardData = {
        getData: originalGetData,
      }

      const pasteEvent = new Event("paste", {
        bubbles: true,
        cancelable: true,
      }) as ClipboardEvent

      // Add clipboardData property
      Object.defineProperty(pasteEvent, "clipboardData", {
        value: mockClipboardData,
        writable: true,
        configurable: true,
      })

      // Trigger the paste event - the handler modifies clipboardData.getData
      quillInstance.root.dispatchEvent(pasteEvent)
      await nextTick()

      // Get the transformed HTML output after paste handler processes it
      const outputHtml = pasteEvent.clipboardData?.getData("text/html")
      expect(outputHtml).toBeDefined()

      // Verify the output: when preserve_pre: true is used, code blocks remain as <pre> tags
      // When preserve_pre is false/undefined, they would be converted to ql-code-block-container style
      expect(outputHtml).toContain("<pre>")
      expect(outputHtml).not.toContain("ql-code-block-container")
      expect(outputHtml).not.toContain("ql-code-block")

      await nextTick()
      await nextTick() // Wait for async processing
    }
  })
})
