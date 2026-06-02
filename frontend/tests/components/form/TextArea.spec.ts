import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { mount } from "@vue/test-utils"
import TextArea from "@/components/form/TextArea.vue"

describe("TextArea.vue", () => {
  const originalGetComputedStyle = window.getComputedStyle
  beforeEach(() => {
    window.getComputedStyle = vi.fn().mockImplementation((elem) => ({
      ...originalGetComputedStyle(elem),
      lineHeight: "20px",
    }))
  })

  afterEach(() => {
    document.body.innerHTML = ""
    window.getComputedStyle = originalGetComputedStyle
  })

  const mountTextArea = (props: Record<string, unknown> = {}) =>
    mount(TextArea, { props, attachTo: document.body })

  const mockScrollHeight = (
    textarea: HTMLTextAreaElement,
    scrollHeight: number
  ) => {
    Object.defineProperty(textarea, "scrollHeight", {
      configurable: true,
      value: scrollHeight,
    })
  }

  const triggerResize = async (
    wrapper: ReturnType<typeof mountTextArea>,
    value: string
  ) => {
    await wrapper.setProps({ modelValue: value })
    await wrapper.vm.$nextTick()
  }

  it("expands rows based on content up to autoExtendUntil", async () => {
    const wrapper = mountTextArea({ autoExtendUntil: 5, rows: 2 })
    const textarea = wrapper.find("textarea").element

    mockScrollHeight(textarea, 80)
    await triggerResize(wrapper, "shorter text")
    expect(textarea.rows).toBe(4)

    mockScrollHeight(textarea, 120)
    await triggerResize(wrapper, "longer text that would exceed limit")
    expect(textarea.rows).toBe(5)

    wrapper.unmount()
  })

  it('emits "enterPressed" when Enter is pressed and enterSubmit is true', async () => {
    const wrapper = mountTextArea({ enterSubmit: true })
    const textareaEl = wrapper.find("textarea").element

    textareaEl.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "Enter",
        bubbles: true,
        cancelable: true,
      })
    )

    expect(wrapper.emitted()).toHaveProperty("enterPressed")
    wrapper.unmount()
  })

  it('does not emit "enterPressed" when Enter is pressed during IME composition', async () => {
    const wrapper = mountTextArea({ enterSubmit: true })
    const textareaEl = wrapper.find("textarea").element

    textareaEl.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "Enter",
        bubbles: true,
        cancelable: true,
        isComposing: true,
      })
    )

    expect(wrapper.emitted()).not.toHaveProperty("enterPressed")
    wrapper.unmount()
  })
})
