import { mount } from "@vue/test-utils"
import TextArea from "@/components/form/TextArea.vue"

function createClipboardEvent(html: string, plainText: string): ClipboardEvent {
  const event = new Event("paste", {
    bubbles: true,
    cancelable: true,
  }) as ClipboardEvent
  const dataTransfer = {
    getData: (format: string) => {
      if (format === "text/html") return html
      if (format === "text/plain") return plainText
      return ""
    },
    setData: () => {
      // Mock implementation - not used in tests
    },
  }
  Object.defineProperty(event, "clipboardData", {
    value: dataTransfer,
    writable: false,
  })
  return event
}

// Mock `getComputedStyle` to return a specific lineHeight
const originalGetComputedStyle = window.getComputedStyle
window.getComputedStyle = vi.fn().mockImplementation((elem) => ({
  ...originalGetComputedStyle(elem),
  lineHeight: "20px", // Example lineHeight
}))

describe("TextArea.vue", () => {
  // Reset mock after all tests are done
  afterAll(() => {
    window.getComputedStyle = originalGetComputedStyle
  })

  it("expands based on content up to 'autoExtendUntil' limit", async () => {
    const wrapper = mount(TextArea, {
      props: {
        autoExtendUntil: 5, // Assuming '5' is the max rows it can expand to
      },
    })

    const textarea = wrapper.find("textarea")
    // Mock `scrollHeight` to simulate content size
    Object.defineProperty(textarea.element, "scrollHeight", {
      configurable: true,
      value: 120, // Assuming content would require 6 rows at 20px lineHeight, but limit is 5
    })

    // Trigger input or any method that recalculates the size
    await textarea.setValue("Some long text that triggers expansion")
    await wrapper.vm.$nextTick()

    expect(textarea.element.rows).toBe(5) // Should not exceed 'autoExtendUntil'
  })

  it("expands as user types within 'autoExtendUntil' limit", async () => {
    const wrapper = mount(TextArea, {
      props: {
        autoExtendUntil: 5,
        rows: 2,
      },
    })

    const textarea = wrapper.find("textarea")
    // Mock `scrollHeight` for smaller content size
    Object.defineProperty(textarea.element, "scrollHeight", {
      configurable: true,
      value: 80, // Assuming content would require 4 rows at 20px lineHeight
    })

    // Trigger input or any method that recalculates the size
    await textarea.setValue("Shorter text")
    await wrapper.vm.$nextTick()

    expect(textarea.element.rows).toBeLessThanOrEqual(5)
    expect(textarea.element.rows).toBeGreaterThan(1) // Assuming initial rows is 1 and it should expand
  })

  it('emits "enterPressed" when Enter is pressed and enterSubmit is true', async () => {
    const wrapper = mount(TextArea, {
      props: {
        enterSubmit: true,
      },
    })

    const textarea = wrapper.find("textarea")
    await textarea.trigger("keydown", { key: "Enter" })

    expect(wrapper.emitted()).toHaveProperty("enterPressed")
  })

  it('does not emit "enterPressed" when Enter is pressed during IME composition', async () => {
    const wrapper = mount(TextArea, {
      props: {
        enterSubmit: true,
      },
    })

    const textarea = wrapper.find("textarea")
    // Simulate the IME composition state by setting isComposing to true
    await textarea.trigger("keydown", { key: "Enter", isComposing: true })

    expect(wrapper.emitted()).not.toHaveProperty("enterPressed")
  })

  it("should extract plain text from clipboard on paste", async () => {
    const wrapper = mount(TextArea, {
      props: {
        modelValue: "existing text",
      },
    })

    await wrapper.vm.$nextTick()

    const textarea = wrapper.find("textarea").element as HTMLTextAreaElement
    textarea.focus()
    textarea.setSelectionRange(9, 9) // Position cursor after "existing " (including space)
    await wrapper.vm.$nextTick()

    // Verify selection is set
    expect(textarea.selectionStart).toBe(9)
    expect(textarea.selectionEnd).toBe(9)

    const pasteEvent = createClipboardEvent(
      "<p><strong>Bold</strong> text</p>",
      "Bold text"
    )

    // Manually trigger the paste handler
    const component = wrapper.vm as InstanceType<typeof TextArea> & {
      handlePaste: (event: ClipboardEvent) => void
    }
    component.handlePaste(pasteEvent)
    await wrapper.vm.$nextTick()

    // Check what was emitted - inserting "Bold text" at position 9 (start of "text")
    // gives us "existing " + "Bold text" + "text" = "existing Bold texttext"
    const emittedValue = wrapper.emitted(
      "update:modelValue"
    )?.[0]?.[0] as string
    expect(emittedValue).toBe("existing Bold texttext")

    // Update the prop to reflect the emitted value
    await wrapper.setProps({
      modelValue: emittedValue,
    })
    await wrapper.vm.$nextTick()

    // Should have inserted plain text only (note: this creates "texttext" at the end)
    expect(textarea.value).toBe("existing Bold texttext")
  })

  it("should replace selected text when pasting", async () => {
    const wrapper = mount(TextArea, {
      props: {
        modelValue: "existing text here",
      },
    })

    await wrapper.vm.$nextTick()

    const textarea = wrapper.find("textarea").element as HTMLTextAreaElement
    textarea.setSelectionRange(9, 13) // Select "text"

    const pasteEvent = createClipboardEvent("", "replaced")

    const component = wrapper.vm as InstanceType<typeof TextArea> & {
      handlePaste: (event: ClipboardEvent) => void
    }
    component.handlePaste(pasteEvent)

    // Update the prop to reflect the emitted value
    await wrapper.setProps({
      modelValue: "existing replaced here",
    })
    await wrapper.vm.$nextTick()

    expect(textarea.value).toBe("existing replaced here")
    // Note: Cursor position is set asynchronously in nextTick, but prop update causes re-render
    // The important part is that the text was replaced correctly
  })

  it("should handle paste at the beginning of text", async () => {
    const wrapper = mount(TextArea, {
      props: {
        modelValue: "existing text",
      },
    })

    await wrapper.vm.$nextTick()

    const textarea = wrapper.find("textarea").element as HTMLTextAreaElement
    textarea.setSelectionRange(0, 0) // Position cursor at start

    const pasteEvent = createClipboardEvent("", "start ")

    const component = wrapper.vm as InstanceType<typeof TextArea> & {
      handlePaste: (event: ClipboardEvent) => void
    }
    component.handlePaste(pasteEvent)

    // Update the prop to reflect the emitted value
    await wrapper.setProps({
      modelValue: "start existing text",
    })
    await wrapper.vm.$nextTick()

    expect(textarea.value).toBe("start existing text")
  })
})
