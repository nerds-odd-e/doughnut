import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { mount } from "@vue/test-utils"
import { page } from "vitest/browser"
import TextArea from "@/components/form/TextArea.vue"

describe("TextArea.vue", () => {
  // Browser Mode: Mock getComputedStyle to return consistent lineHeight (20px)
  // This matches the JSDOM test behavior while still using real browser APIs for everything else
  const originalGetComputedStyle = window.getComputedStyle
  beforeEach(() => {
    window.getComputedStyle = vi.fn().mockImplementation((elem) => ({
      ...originalGetComputedStyle(elem),
      lineHeight: "20px", // Match JSDOM test's mocked lineHeight
    }))
  })

  afterEach(() => {
    document.body.innerHTML = ""
    window.getComputedStyle = originalGetComputedStyle
  })

  it("expands based on content up to 'autoExtendUntil' limit", async () => {
    const wrapper = mount(TextArea, {
      props: {
        autoExtendUntil: 5, // Assuming '5' is the max rows it can expand to
      },
      attachTo: document.body,
    })

    const textarea = wrapper.find("textarea")

    // Browser Mode: Set scrollHeight to simulate content that would require more rows than limit
    // With lineHeight = 20px: 120 / 20 = 6 rows, but should cap at 5
    Object.defineProperty(textarea.element, "scrollHeight", {
      configurable: true,
      value: 120, // Assuming content would require 6 rows at 20px lineHeight, but limit is 5
    })

    // Trigger input using page interaction
    const textareaLocator = page.getByRole("textbox")
    await textareaLocator.fill("Some long text that triggers expansion")
    await wrapper.vm.$nextTick()
    // No manual waiting needed, expect.element handles it

    // Browser Mode: Should be exactly 5 (capped at autoExtendUntil limit)
    // Use vi.waitUntil to wait for the rows property to update
    await vi.waitUntil(() => textarea.element.rows === 5, {
      timeout: 1000,
      interval: 20,
    })

    wrapper.unmount()
  })

  it("expands as user types within 'autoExtendUntil' limit", async () => {
    const wrapper = mount(TextArea, {
      props: {
        autoExtendUntil: 5,
        rows: 2,
      },
      attachTo: document.body,
    })

    const textarea = wrapper.find("textarea")
    // Browser Mode: Use real scrollHeight by setting actual content
    Object.defineProperty(textarea.element, "scrollHeight", {
      configurable: true,
      value: 80, // Assuming content would require 4 rows at 20px lineHeight
    })

    // Trigger input using page interaction
    const textareaLocator = page.getByRole("textbox")
    await textareaLocator.fill("Shorter text")
    await wrapper.vm.$nextTick()
    // No manual waiting needed, expect.element handles it

    expect(textarea.element.rows).toBeLessThanOrEqual(5)
    expect(textarea.element.rows).toBeGreaterThan(1) // Assuming initial rows is 1 and it should expand

    wrapper.unmount()
  })

  it('emits "enterPressed" when Enter is pressed and enterSubmit is true', async () => {
    const wrapper = mount(TextArea, {
      props: {
        enterSubmit: true,
      },
      attachTo: document.body,
    })

    const textareaLocator = page.getByRole("textbox")
    await expect.element(textareaLocator).toBeVisible()

    // Browser Mode: Real keyboard events work!
    // Use DOM dispatch since .press() might not be available on this locator type
    const textareaEl = document.querySelector("textarea") as HTMLTextAreaElement
    if (!textareaEl) throw new Error("Textarea not found")

    const event = new KeyboardEvent("keydown", {
      key: "Enter",
      bubbles: true,
      cancelable: true,
    })
    textareaEl.dispatchEvent(event)

    expect(wrapper.emitted()).toHaveProperty("enterPressed")
    wrapper.unmount()
  })

  it('does not emit "enterPressed" when Enter is pressed during IME composition', async () => {
    const wrapper = mount(TextArea, {
      props: {
        enterSubmit: true,
      },
      attachTo: document.body,
    })

    // Browser Mode: Real keyboard events with IME composition state!
    // We need to access the element to dispatch a custom event with isComposing
    const textareaEl = document.querySelector("textarea") as HTMLTextAreaElement
    const event = new KeyboardEvent("keydown", {
      key: "Enter",
      bubbles: true,
      cancelable: true,
      isComposing: true,
    })
    textareaEl.dispatchEvent(event)

    expect(wrapper.emitted()).not.toHaveProperty("enterPressed")
    wrapper.unmount()
  })
})
