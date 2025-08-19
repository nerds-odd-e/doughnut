import { mount } from "@vue/test-utils"
import QuillEditor from "@/components/form/QuillEditor.vue"
import { nextTick } from "vue"
import type Quill from "quill"

describe("QuillEditor.vue", () => {
  it.skip("renders HTML table content", async () => {
    const html = `<table><tr><td>Name</td><td>Score</td></tr><tr><td>Alice</td><td>95</td></tr></table>`
    const wrapper = mount(QuillEditor, {
      props: { modelValue: html },
    })
    await nextTick() // wait for Quill to initialize
    // Quill renders content inside .ql-editor
    const table = wrapper.element.querySelector(".ql-editor table")
    expect(table).not.toBeNull()
    expect(table?.querySelectorAll("tr").length).toBe(2)
    expect(table?.textContent).toContain("Alice")
    expect(table?.textContent).toContain("95")
  })

  it("renders simple HTML content", async () => {
    const html = `<h1>Hello</h1><p>World</p>`
    const wrapper = mount(QuillEditor, {
      props: { modelValue: html },
    })
    await nextTick()

    // Wait for Quill initialization and content loading (our setTimeout is 100ms)
    await new Promise((resolve) => setTimeout(resolve, 150))

    const h1 = wrapper.element.querySelector(".ql-editor h1")
    const p = wrapper.element.querySelector(".ql-editor p")
    expect(h1).not.toBeNull()
    expect(h1?.textContent).toBe("Hello")
    expect(p).not.toBeNull()
    expect(p?.textContent).toBe("World")
  })

  it("does not emit update on initial programmatic load (prevents cursor jump)", async () => {
    const html = `<p>abc</p>`
    const wrapper = mount(QuillEditor, {
      props: { modelValue: html },
    })
    await nextTick()
    // No user change yet, so no emit
    expect(wrapper.emitted("update:modelValue")).toBeFalsy()
  })

  it("keeps selection stable across external modelValue updates", async () => {
    const html = `<p>Hello World</p>`
    const wrapper = mount(QuillEditor, {
      props: { modelValue: html },
    })
    await nextTick()

    // Simulate user placing cursor at end
    const quillInstance = (wrapper.vm as { quill: Quill }).quill
    // Ensure editor has focus before setting selection in jsdom
    ;(wrapper.element.querySelector(".ql-editor") as HTMLElement).focus()
    quillInstance.setSelection(quillInstance.getLength() - 1, 0, "user")

    const prevSelection = quillInstance.getSelection()

    // External update (e.g., parent updates modelValue). Should not reset selection.
    await wrapper.setProps({ modelValue: `<p>Hello World!!!</p>` })
    await nextTick()

    const afterSelection = quillInstance.getSelection()
    expect(afterSelection?.index).toBe(prevSelection?.index)
    expect(afterSelection?.length).toBe(prevSelection?.length)
  })
})
