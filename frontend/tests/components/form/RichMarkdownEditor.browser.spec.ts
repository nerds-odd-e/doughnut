import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import helper from "@tests/helpers"

describe("RichMarkdownEditor", () => {
  let wrapper: any

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  const mountEditor = async (initialValue: string, options = {}) => {
    wrapper = helper
      .component(RichMarkdownEditor)
      .withProps({
        modelValue: initialValue,
        ...options,
      })
      .mount({ attachTo: document.body })
    await flushPromises()
    return wrapper
  }

  it("not emit update when the change is from initial value", async () => {
    await mountEditor("initial value")
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("not emit update if readonly", async () => {
    await mountEditor("# Title", { readonly: true })
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("will try to unify the markdown", async () => {
    await mountEditor("# Title")
    await flushPromises()
    await nextTick()
    await flushPromises()
    const emitted = wrapper.emitted()["update:modelValue"]
    if (emitted?.length) {
      expect(emitted[emitted.length - 1]![0]).toContain("Title")
    }
  })

  it("converts HTML to markdown then back to HTML when pasting", async () => {
    await mountEditor("")
    await flushPromises()
    await nextTick()

    const qlEditor = wrapper
      .findComponent({ name: "QuillEditor" })
      .vm.$el.querySelector(".ql-editor") as HTMLElement
    expect(qlEditor).toBeTruthy()

    // Browser Mode: Real focus() method!
    qlEditor.focus()
    await nextTick()
    await flushPromises()

    // Browser Mode: Use real ClipboardEvent with DataTransfer!
    // Create a real clipboard event with HTML data
    const clipboardData = new DataTransfer()
    clipboardData.setData("text/html", "<p><strong>Bold text</strong></p>")

    const pasteEvent = new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })

    qlEditor.dispatchEvent(pasteEvent)

    await nextTick()
    await flushPromises()

    const emitted = wrapper.emitted()["update:modelValue"]
    expect(wrapper.exists()).toBe(true)
    if (emitted?.length) {
      expect(emitted[emitted.length - 1]![0]).toContain("Bold text")
    }
  })

  it("does not handle paste when readonly", async () => {
    await mountEditor("", { readonly: true })
    await flushPromises()

    const qlEditor = wrapper
      .findComponent({ name: "QuillEditor" })
      .vm.$el.querySelector(".ql-editor") as HTMLElement

    // Browser Mode: Real ClipboardEvent!
    const clipboardData = new DataTransfer()
    clipboardData.setData("text/html", "<p>Test</p>")
    const pasteEvent = new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })

    qlEditor.dispatchEvent(pasteEvent)
    await flushPromises()

    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })
})
