import { mount } from "@vue/test-utils"
import { nextTick } from "vue"
import SeamlessTextEditor from "@/components/form/SeamlessTextEditor.vue"

// Helper to create a mock ClipboardEvent
function createPasteEvent(
  plainText: string,
  htmlText?: string
): ClipboardEvent {
  // Create a mock DataTransfer
  const dataTransfer = {
    getData: (format: string) => {
      if (format === "text/plain") {
        return plainText
      }
      if (format === "text/html" && htmlText) {
        return htmlText
      }
      return ""
    },
    setData: () => {
      // Mock implementation - not used in tests
    },
    clearData: () => {
      // Mock implementation - not used in tests
    },
    setDragImage: () => {
      // Mock implementation - not used in tests
    },
    files: [] as unknown as FileList,
    items: [] as unknown as DataTransferItemList,
    types: [] as unknown as DOMStringList,
    effectAllowed: "uninitialized" as DataTransfer["effectAllowed"],
    dropEffect: "none" as DataTransfer["dropEffect"],
  } as unknown as DataTransfer

  // Create a mock ClipboardEvent
  const event = new Event("paste", {
    bubbles: true,
    cancelable: true,
  }) as ClipboardEvent

  // Add clipboardData property
  Object.defineProperty(event, "clipboardData", {
    value: dataTransfer,
    writable: false,
  })

  return event
}

describe("SeamlessTextEditor.vue", () => {
  it("should extract plain text from clipboard on paste and prevent HTML formatting", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "Original text",
        readonly: false,
      },
    })

    await nextTick()

    const editor = wrapper.find(".seamless-editor")
    const editorElement = editor.element as HTMLElement

    // Focus the editor and set up selection at the end
    editorElement.focus()
    const range = document.createRange()
    const selection = window.getSelection()
    if (selection && editorElement.firstChild) {
      range.setStart(
        editorElement.firstChild,
        editorElement.firstChild.textContent?.length || 0
      )
      range.collapse(true)
      selection.removeAllRanges()
      selection.addRange(range)
    }

    // Create a paste event with HTML content
    const pasteEvent = createPasteEvent(
      "Bold text and italic",
      "<b>Bold text</b> and <i>italic</i>"
    )

    // Dispatch the paste event directly on the element
    editorElement.dispatchEvent(pasteEvent)
    await nextTick()

    // Verify that update:modelValue was emitted with plain text
    const updateEvents = wrapper.emitted("update:modelValue")
    expect(updateEvents).toBeTruthy()
    const lastEvent = updateEvents?.[updateEvents.length - 1]
    expect(lastEvent).toEqual(["Original textBold text and italic"])

    // Update the prop to match what was emitted, then verify DOM
    await wrapper.setProps({ modelValue: "Original textBold text and italic" })
    await nextTick()

    // Verify that only plain text was inserted, not HTML
    expect(editorElement.innerText).toBe("Original textBold text and italic")
    expect(editorElement.innerHTML).not.toContain("<b>")
    expect(editorElement.innerHTML).not.toContain("<i>")
  })

  it("should handle paste when clipboard contains only plain text", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "Start",
        readonly: false,
      },
    })

    await nextTick()

    const editor = wrapper.find(".seamless-editor")
    const editorElement = editor.element as HTMLElement

    // Focus the editor and set up selection at the end
    editorElement.focus()
    const range = document.createRange()
    const selection = window.getSelection()
    if (selection && editorElement.firstChild) {
      range.setStart(
        editorElement.firstChild,
        editorElement.firstChild.textContent?.length || 0
      )
      range.collapse(true)
      selection.removeAllRanges()
      selection.addRange(range)
    }

    // Create paste event with only plain text
    const pasteEvent = createPasteEvent(" end")

    editorElement.dispatchEvent(pasteEvent)
    await nextTick()

    // Verify emission
    expect(wrapper.emitted("update:modelValue")).toBeTruthy()
    const lastEvent =
      wrapper.emitted("update:modelValue")?.[
        wrapper.emitted("update:modelValue")!.length - 1
      ]
    expect(lastEvent).toEqual(["Start end"])

    // Update prop and verify DOM
    await wrapper.setProps({ modelValue: "Start end" })
    await nextTick()
    expect(editorElement.innerText).toBe("Start end")
  })

  it("should handle paste when replacing selected text", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "Hello world",
        readonly: false,
      },
    })

    await nextTick()

    const editor = wrapper.find(".seamless-editor")
    const editorElement = editor.element as HTMLElement

    // Select "world" - ensure editor is focused first
    editorElement.focus()
    await nextTick()

    const range = document.createRange()
    const selection = window.getSelection()
    const textNode = editorElement.firstChild as Text
    if (!textNode || !selection) {
      throw new Error("Text node or selection not found")
    }

    // Set selection from position 6 to 11 (selecting "world")
    range.setStart(textNode, 6) // After "Hello "
    range.setEnd(textNode, 11) // End of "world"
    selection.removeAllRanges()
    selection.addRange(range)

    // Paste to replace selection
    const pasteEvent = createPasteEvent("universe", "<b>universe</b>")

    editorElement.dispatchEvent(pasteEvent)
    await nextTick()

    // Verify emission
    const updateEvents = wrapper.emitted("update:modelValue")
    expect(updateEvents).toBeTruthy()
    const lastEvent = updateEvents?.[updateEvents.length - 1]
    expect(lastEvent).toEqual(["Hello universe"])

    // Update prop and verify DOM
    await wrapper.setProps({ modelValue: "Hello universe" })
    await nextTick()
    expect(editorElement.innerText).toBe("Hello universe")
    expect(editorElement.innerHTML).not.toContain("<b>")
  })

  it("should not process paste when readonly", async () => {
    const wrapper = mount(SeamlessTextEditor, {
      props: {
        modelValue: "Readonly text",
        readonly: true,
      },
    })

    await nextTick()

    const editor = wrapper.find(".seamless-editor")
    const editorElement = editor.element as HTMLElement

    // Try to paste
    const pasteEvent = createPasteEvent("new text")

    // In readonly mode, contenteditable is false, so paste shouldn't work
    // But let's verify the handler doesn't break
    editorElement.dispatchEvent(pasteEvent)
    await nextTick()
    await nextTick() // Wait for watch to complete

    // Content should remain unchanged (readonly prevents editing)
    expect(editorElement.innerText).toBe("Readonly text")
  })
})
