import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"

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

  it.skip("will try to unify the markdown", async () => {
    const wrapper = await mountEditor("**」**x")
    await flushPromises()
    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted![0]![0]).toEqual("Title\n=====")
  })
})
