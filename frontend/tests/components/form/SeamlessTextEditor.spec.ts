import { mount } from "@vue/test-utils"
import SeamlessTextEditor from "@/components/form/SeamlessTextEditor.vue"
import { nextTick } from "vue"

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
  return event
}

describe("SeamlessTextEditor.vue", () => {
  it("emits update:modelValue on input", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "initial",
      },
    })

    const editor = wrapper.find(".seamless-editor")
    ;(editor.element as HTMLElement).innerText = "new text"
    await editor.trigger("input")

    expect(wrapper.emitted("update:modelValue")).toEqual([["new text"]])
  })

  it("emits blur event on blur", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "test",
      },
    })

    const editor = wrapper.find(".seamless-editor")
    await editor.trigger("blur")

    expect(wrapper.emitted("blur")).toBeTruthy()
  })

  it("dispatches blur event on Enter key", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "test",
      },
    })

    const editor = wrapper.find(".seamless-editor")
    const blurSpy = vi.fn()
    editor.element.addEventListener("blur", blurSpy)

    await editor.trigger("keydown.enter")

    expect(blurSpy).toHaveBeenCalled()
  })

  it("updates content when modelValue changes", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "initial",
      },
    })

    await wrapper.setProps({ modelValue: "updated" })
    await nextTick()

    const editor = wrapper.find(".seamless-editor")
    expect((editor.element as HTMLElement).innerText).toBe("updated")
  })

  it("extracts plain text from clipboard on paste", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "initial",
      },
    })
    await nextTick()

    const editor = wrapper.find(".seamless-editor")
    const editorElement = editor.element as HTMLElement

    // Set up selection - select all content using the component's text node
    editorElement.focus()
    const textNode = editorElement.firstChild as Text
    if (!textNode) {
      throw new Error("Text node not found")
    }
    const range = document.createRange()
    range.selectNodeContents(textNode)
    const selection = window.getSelection()
    if (selection) {
      selection.removeAllRanges()
      selection.addRange(range)
    }

    // Create paste event with HTML data
    const pasteEvent = createClipboardEvent("bold text", "<b>bold</b> text")

    editorElement.dispatchEvent(pasteEvent)
    await nextTick()

    // Should extract plain text, not HTML
    expect(editorElement.innerText).toBe("bold text")
    expect(wrapper.emitted("update:modelValue")).toEqual([["bold text"]])
  })

  it("pastes plain text at cursor position", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "hello world",
      },
    })
    await nextTick()

    const editor = wrapper.find(".seamless-editor")
    const editorElement = editor.element as HTMLElement

    // Focus the editor and set cursor after "hello "
    editorElement.focus()
    const textNode = editorElement.firstChild as Text
    if (!textNode) {
      throw new Error("Text node not found")
    }
    const range = document.createRange()
    range.setStart(textNode, 6) // After "hello "
    range.setEnd(textNode, 6)
    const selection = window.getSelection()
    if (selection) {
      selection.removeAllRanges()
      selection.addRange(range)
    }

    const pasteEvent = createClipboardEvent("beautiful ")

    editorElement.dispatchEvent(pasteEvent)
    await nextTick()

    expect(editorElement.innerText).toBe("hello beautiful world")
    expect(wrapper.emitted("update:modelValue")).toEqual([
      ["hello beautiful world"],
    ])
  })

  it("replaces selected text when pasting", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "hello world",
      },
    })
    await nextTick()

    const editor = wrapper.find(".seamless-editor")
    const editorElement = editor.element as HTMLElement

    // Focus the editor and select "world"
    editorElement.focus()
    const textNode = editorElement.firstChild as Text
    if (!textNode) {
      throw new Error("Text node not found")
    }
    const range = document.createRange()
    range.setStart(textNode, 6) // Start of "world"
    range.setEnd(textNode, 11) // End of "world"
    const selection = window.getSelection()
    if (selection) {
      selection.removeAllRanges()
      selection.addRange(range)
    }

    const pasteEvent = createClipboardEvent("universe")

    editorElement.dispatchEvent(pasteEvent)
    await nextTick()

    expect(editorElement.innerText).toBe("hello universe")
    expect(wrapper.emitted("update:modelValue")).toEqual([["hello universe"]])
  })

  it("appends text when no selection exists", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "initial",
      },
    })

    const editor = wrapper.find(".seamless-editor")
    const editorElement = editor.element as HTMLElement

    // Clear selection
    const selection = window.getSelection()
    selection?.removeAllRanges()

    const pasteEvent = createClipboardEvent(" appended")

    editorElement.dispatchEvent(pasteEvent)
    await nextTick()

    expect(editorElement.innerText).toBe("initial appended")
    expect(wrapper.emitted("update:modelValue")).toEqual([["initial appended"]])
  })

  it("handles paste with empty clipboard gracefully", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "initial",
      },
    })

    const editor = wrapper.find(".seamless-editor")
    const editorElement = editor.element as HTMLElement

    const pasteEvent = createClipboardEvent("")

    editorElement.dispatchEvent(pasteEvent)
    await nextTick()

    // Should not crash and content should remain unchanged
    expect(editorElement.innerText).toBe("initial")
  })

  it("strips HTML formatting from pasted content", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "",
      },
    })

    const editor = wrapper.find(".seamless-editor")
    const editorElement = editor.element as HTMLElement

    const pasteEvent = createClipboardEvent(
      "This is bold and italic text",
      "<p>This is <strong>bold</strong> and <em>italic</em> text</p>"
    )

    editorElement.dispatchEvent(pasteEvent)
    await nextTick()

    // Should only contain plain text, no HTML tags
    expect(editorElement.innerText).toBe("This is bold and italic text")
    expect(editorElement.innerHTML).not.toContain("<strong>")
    expect(editorElement.innerHTML).not.toContain("<em>")
    expect(editorElement.innerHTML).not.toContain("<p>")
  })

  it("is readonly when readonly prop is true", () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "test",
        readonly: true,
      },
    })

    const editor = wrapper.find(".seamless-editor")
    expect(editor.attributes("contenteditable")).toBe("false")
  })

  it("is editable when readonly prop is false", () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "test",
        readonly: false,
      },
    })

    const editor = wrapper.find(".seamless-editor")
    expect(editor.attributes("contenteditable")).toBe("true")
  })
})
