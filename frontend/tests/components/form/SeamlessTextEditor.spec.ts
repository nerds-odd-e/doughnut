import SeamlessTextEditor from "@/components/form/SeamlessTextEditor.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { nextTick } from "vue"
import helper from "@tests/helpers"

describe("SeamlessTextEditor", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  const mountEditor = async (initialValue: string, options = {}) => {
    wrapper = helper
      .component(SeamlessTextEditor)
      .withProps({
        modelValue: initialValue,
        ...options,
      })
      .mount({ attachTo: document.body })
    await flushPromises()
    await nextTick()
    return wrapper
  }

  it("does not emit update when the change is from initial value", async () => {
    await mountEditor("initial value")
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("extracts plain text when pasting HTML content", async () => {
    await mountEditor("")
    await flushPromises()
    await nextTick()

    const editor = wrapper.find(".seamless-editor").element as HTMLElement
    expect(editor).toBeTruthy()

    // Browser Mode: Real focus() method!
    editor.focus()
    await nextTick()
    await flushPromises()

    // Browser Mode: Use real ClipboardEvent with DataTransfer!
    const clipboardData = new DataTransfer()
    clipboardData.setData("text/plain", "Bold text")
    clipboardData.setData("text/html", "<p><strong>Bold text</strong></p>")

    const pasteEvent = new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })

    editor.dispatchEvent(pasteEvent)
    await nextTick()
    await flushPromises()

    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted).toBeDefined()
    expect(emitted?.length).toBeGreaterThan(0)
    expect(emitted?.[emitted.length - 1]?.[0]).toBe("Bold text")
    expect(editor.innerText).toBe("Bold text")
  })

  it("pastes plain text at cursor position", async () => {
    await mountEditor("existing text")
    await flushPromises()
    await nextTick()

    const editor = wrapper.find(".seamless-editor").element as HTMLElement
    // Browser Mode: Real focus() method!
    editor.focus()
    await nextTick()
    await flushPromises()

    // Browser Mode: Real Selection API!
    // Set cursor position after "existing" (8 characters)
    const textNode = editor.firstChild as Text
    expect(textNode).toBeTruthy()

    // Browser Mode: Use real window.getSelection() and document.createRange()
    const selection = window.getSelection()
    const range = document.createRange()
    if (textNode) {
      range.setStart(textNode, 8) // After "existing"
      range.setEnd(textNode, 8)
      selection?.removeAllRanges()
      selection?.addRange(range)
    }

    // Browser Mode: Real ClipboardEvent!
    const clipboardData = new DataTransfer()
    clipboardData.setData("text/plain", " inserted")
    const pasteEvent = new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })

    editor.dispatchEvent(pasteEvent)
    await nextTick()
    await flushPromises()

    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted).toBeDefined()
    expect(emitted?.length).toBeGreaterThan(0)
    // Verify paste worked - text should contain both original and pasted content
    const finalText = emitted?.[emitted.length - 1]?.[0] as string
    expect(finalText).toContain("existing")
    expect(finalText).toContain("inserted")
    expect(finalText).toContain("text")
    expect(editor.innerText).toContain("existing")
    expect(editor.innerText).toContain("inserted")
  })

  it("appends plain text when no selection", async () => {
    await mountEditor("existing")
    await flushPromises()
    await nextTick()

    const editor = wrapper.find(".seamless-editor").element as HTMLElement
    // Browser Mode: Real focus() method!
    editor.focus()
    await nextTick()

    // Browser Mode: Real Selection API!
    // Clear selection
    window.getSelection()?.removeAllRanges()

    // Browser Mode: Real ClipboardEvent!
    const clipboardData = new DataTransfer()
    clipboardData.setData("text/plain", " appended")
    const pasteEvent = new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })

    editor.dispatchEvent(pasteEvent)
    await nextTick()
    await flushPromises()

    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted).toBeDefined()
    expect(emitted?.length).toBeGreaterThan(0)
    expect(emitted?.[emitted.length - 1]?.[0]).toBe("existing appended")
    expect(editor.innerText).toBe("existing appended")
  })

  it("does not handle paste when readonly", async () => {
    await mountEditor("", { readonly: true })
    await flushPromises()
    await nextTick()

    const editor = wrapper.find(".seamless-editor").element as HTMLElement
    // Browser Mode: Real focus() method!
    editor.focus()
    await nextTick()

    // Browser Mode: Real ClipboardEvent!
    const clipboardData = new DataTransfer()
    clipboardData.setData("text/plain", "Test")
    const pasteEvent = new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })

    editor.dispatchEvent(pasteEvent)
    await flushPromises()

    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("handles paste with empty clipboard", async () => {
    await mountEditor("existing")
    await flushPromises()
    await nextTick()

    const editor = wrapper.find(".seamless-editor").element as HTMLElement
    // Browser Mode: Real focus() method!
    editor.focus()
    await nextTick()

    // Browser Mode: Real ClipboardEvent with empty data!
    const clipboardData = new DataTransfer()
    const pasteEvent = new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })

    editor.dispatchEvent(pasteEvent)
    await nextTick()
    await flushPromises()

    // Should not emit update when clipboard is empty
    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted).toBeUndefined()
    expect(editor.innerText).toBe("existing")
  })
})
