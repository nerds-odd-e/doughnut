import SeamlessTextEditor from "@/components/form/SeamlessTextEditor.vue"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import helper from "@tests/helpers"

function createClipboardEvent(
  plainText: string,
  html?: string
): ClipboardEvent {
  const event = new Event("paste", {
    bubbles: true,
    cancelable: true,
  }) as ClipboardEvent
  const dataTransfer = {
    getData: (format: string) => {
      if (format === "text/plain") return plainText
      if (format === "text/html" && html) return html
      return ""
    },
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

describe("SeamlessTextEditor", () => {
  const mountEditor = async (initialValue: string, options = {}) => {
    const wrapper = helper
      .component(SeamlessTextEditor)
      .withProps({
        modelValue: initialValue,
        ...options,
      })
      .mount()
    await flushPromises()
    await nextTick()
    return wrapper
  }

  it("does not emit update when the change is from initial value", async () => {
    const wrapper = await mountEditor("initial value")
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("extracts plain text when pasting HTML content", async () => {
    const wrapper = await mountEditor("")
    await flushPromises()
    await nextTick()

    const editor = wrapper.find(".seamless-editor").element as HTMLElement
    expect(editor).toBeTruthy()

    editor.focus()
    await nextTick()
    await flushPromises()

    const pasteEvent = createClipboardEvent(
      "Bold text",
      "<p><strong>Bold text</strong></p>"
    )

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
    const wrapper = await mountEditor("existing text")
    await flushPromises()
    await nextTick()

    const editor = wrapper.find(".seamless-editor").element as HTMLElement
    editor.focus()
    await nextTick()
    await flushPromises()

    // Set cursor position after "existing" (8 characters)
    const textNode = editor.firstChild as Text
    expect(textNode).toBeTruthy()

    // Set selection right before paste
    const selection = window.getSelection()
    const range = document.createRange()
    if (textNode) {
      range.setStart(textNode, 8) // After "existing"
      range.setEnd(textNode, 8)
      selection?.removeAllRanges()
      selection?.addRange(range)
    }

    const pasteEvent = createClipboardEvent(" inserted")
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
    const wrapper = await mountEditor("existing")
    await flushPromises()
    await nextTick()

    const editor = wrapper.find(".seamless-editor").element as HTMLElement
    editor.focus()
    await nextTick()

    // Clear selection
    window.getSelection()?.removeAllRanges()

    const pasteEvent = createClipboardEvent(" appended")
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
    const wrapper = await mountEditor("", { readonly: true })
    await flushPromises()
    await nextTick()

    const editor = wrapper.find(".seamless-editor").element as HTMLElement
    editor.focus()
    await nextTick()

    const pasteEvent = createClipboardEvent("Test")
    editor.dispatchEvent(pasteEvent)
    await flushPromises()

    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("handles paste with empty clipboard", async () => {
    const wrapper = await mountEditor("existing")
    await flushPromises()
    await nextTick()

    const editor = wrapper.find(".seamless-editor").element as HTMLElement
    editor.focus()
    await nextTick()

    const pasteEvent = createClipboardEvent("")
    editor.dispatchEvent(pasteEvent)
    await nextTick()
    await flushPromises()

    // Should not emit update when clipboard is empty
    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted).toBeUndefined()
    expect(editor.innerText).toBe("existing")
  })
})
