import { mount } from "@vue/test-utils"
import QuillEditor from "@/components/form/QuillEditor.vue"
import { nextTick } from "vue"

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
})
