import { mount } from "@vue/test-utils"
import QuillEditor from "@/components/form/QuillEditor.vue"
import { nextTick } from "vue"

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
    const h1 = wrapper.element.querySelector(".ql-editor h1")
    const p = wrapper.element.querySelector(".ql-editor p")
    expect(h1).not.toBeNull()
    expect(h1?.textContent).toBe("Hello")
    expect(p).not.toBeNull()
    expect(p?.textContent).toBe("World")
  })
})
