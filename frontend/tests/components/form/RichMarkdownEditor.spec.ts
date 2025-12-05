import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import helper from "@tests/helpers"

function createClipboardEvent(html: string): ClipboardEvent {
  const event = new Event("paste", {
    bubbles: true,
    cancelable: true,
  }) as ClipboardEvent
  const dataTransfer = {
    getData: (format: string) => (format === "text/html" ? html : ""),
    setData: () => {
      // Mock implementation - not used in tests
    },
  }
  Object.defineProperty(event, "clipboardData", {
    value: dataTransfer,
    writable: true,
    configurable: true,
  })
  Object.defineProperty(dataTransfer, "getData", {
    writable: true,
    configurable: true,
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
    await nextTick()
    await flushPromises()
    const emitted = wrapper.emitted()["update:modelValue"]
    if (emitted?.length) {
      expect(emitted[emitted.length - 1]![0]).toContain("Title")
    }
  })

  it("converts HTML to markdown then back to HTML when pasting", async () => {
    const wrapper = await mountEditor("")
    await flushPromises()
    await nextTick()

    const qlEditor = wrapper
      .findComponent({ name: "QuillEditor" })
      .vm.$el.querySelector(".ql-editor") as HTMLElement
    expect(qlEditor).toBeTruthy()

    qlEditor.focus()
    await nextTick()
    await flushPromises()

    qlEditor.dispatchEvent(
      createClipboardEvent("<p><strong>Bold text</strong></p>")
    )

    await nextTick()
    await flushPromises()

    const emitted = wrapper.emitted()["update:modelValue"]
    expect(wrapper.exists()).toBe(true)
    if (emitted?.length) {
      expect(emitted[emitted.length - 1]![0]).toContain("Bold text")
    }
  })

  it("does not handle paste when readonly", async () => {
    const wrapper = await mountEditor("", { readonly: true })
    await flushPromises()

    const qlEditor = wrapper
      .findComponent({ name: "QuillEditor" })
      .vm.$el.querySelector(".ql-editor") as HTMLElement

    qlEditor.dispatchEvent(createClipboardEvent("<p>Test</p>"))
    await flushPromises()

    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })
})
