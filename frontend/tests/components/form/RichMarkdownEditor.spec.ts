import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import { flushPromises, mount } from "@vue/test-utils"
import { nextTick } from "vue"

describe("RichMarkdownEditor", () => {
  const mountEditor = async (initialValue: string, options = {}) => {
    const wrapper = mount(RichMarkdownEditor, {
      props: {
        modelValue: initialValue,
        ...options,
      },
    })
    await nextTick()
    await flushPromises()

    // Wait for Quill initialization and content loading (QuillEditor setTimeout is 100ms)
    await new Promise((resolve) => setTimeout(resolve, 150))

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

  it("renders markdown headers correctly", async () => {
    const wrapper = await mountEditor("# Title")

    // The component should render "# Title" as an <h1> element
    const h1Element = wrapper.find("h1")
    expect(h1Element.exists()).toBe(true)
    expect(h1Element.text()).toBe("Title")

    // Also check if the component is properly converting markdown to HTML
    // The input was "# Title" and it should render as <h1>Title</h1>
    const editorContent = wrapper.find(".ql-editor")
    expect(editorContent.exists()).toBe(true)
    expect(editorContent.html()).toContain("<h1>Title</h1>")
  })

  it.skip("will try to unify the markdown", async () => {
    const wrapper = await mountEditor("**」**x")
    await flushPromises()
    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted![0]![0]).toEqual("Title\n=====")
  })
})
