import { mount } from "@vue/test-utils"
import QuillEditor from "@/components/form/QuillEditor.vue"
import { nextTick } from "vue"
import type Quill from "quill"

describe("QuillEditor.vue", () => {
  it("renders simple HTML content", async () => {
    const html = `<h1>Hello</h1><p>World</p>`
    const wrapper = mount(QuillEditor, {
      props: { modelValue: html },
    })
    await nextTick()
    const h1 = wrapper.element.querySelector(".ql-editor h1")
    const p = wrapper.element.querySelector(".ql-editor p")
    expect(h1).not.toBeNull()
    expect(h1?.textContent).toBe("Hello")
    expect(p).not.toBeNull()
    expect(p?.textContent).toBe("World")
  })

  it("emits correct modelValue when typing Hello<Shift+Enter>World", async () => {
    const wrapper = mount(QuillEditor, {
      props: { modelValue: "" },
    })
    await nextTick()
    await nextTick() // Wait for Quill to fully initialize

    // Access the Quill instance from the component
    // biome-ignore lint/suspicious/noExplicitAny: Quill instance is not part of the public API
    const quillInstance = (wrapper.vm as any).quill as Quill | null
    expect(quillInstance).not.toBeNull()

    if (quillInstance) {
      // Focus the editor
      quillInstance.focus()
      await nextTick()

      quillInstance.clipboard.dangerouslyPasteHTML(
        0,
        "<p>Hello<br>World</p>",
        "user"
      )
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
})
