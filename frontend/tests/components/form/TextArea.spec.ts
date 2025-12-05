import { mount } from "@vue/test-utils"
import TextArea from "@/components/form/TextArea.vue"

// Mock `getComputedStyle` to return a specific lineHeight
const originalGetComputedStyle = window.getComputedStyle
window.getComputedStyle = vi.fn().mockImplementation((elem) => ({
  ...originalGetComputedStyle(elem),
  lineHeight: "20px", // Example lineHeight
}))

function createClipboardEvent(html: string): ClipboardEvent {
  const event = new Event("paste", {
    bubbles: true,
    cancelable: true,
  }) as ClipboardEvent
  const dataTransfer = {
    getData: (format: string) => {
      if (format === "text/html") return html
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

  it("converts HTML to markdown when pasting HTML content", async () => {
    const wrapper = mount(TextArea, {
      props: {
        modelValue: "existing text",
      },
    })

    const textarea = wrapper.find("textarea").element as HTMLTextAreaElement
    textarea.setSelectionRange(8, 8) // Position cursor after "existing"

    const pasteEvent = createClipboardEvent("<p><strong>Bold text</strong></p>")

    await textarea.dispatchEvent(pasteEvent)
    await wrapper.vm.$nextTick()

    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted).toBeDefined()
    const lastEmitted = emitted![emitted!.length - 1]![0] as string
    // Should contain the markdown version of the HTML
    expect(lastEmitted).toContain("Bold text")
    expect(lastEmitted).toContain("existing")
  })
})
