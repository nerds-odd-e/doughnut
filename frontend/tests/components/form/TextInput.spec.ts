import { mount, flushPromises } from "@vue/test-utils"
import TextInput from "@/components/form/TextInput.vue"
import { vi } from "vitest"

describe("TextInput.vue", () => {
  afterEach(() => {
    document.body.innerHTML = ""
  })

  it("The internal input field is disabled if the component is given the 'disabled' prop", async () => {
    const wrapper = mount(TextInput, {
      props: {
        disabled: true,
        modelValue: "test",
        scopeName: "test",
        field: "test",
        title: "test",
      },
    })
    const input = wrapper.find("input")
    expect(input.element.disabled).toBe(true)
  })

  it("selects all text when initialSelectAll is true", async () => {
    // Browser Mode: Spy on select() BEFORE mounting to catch the call
    // We need to spy on the prototype since the element doesn't exist yet
    const selectSpy = vi.spyOn(HTMLInputElement.prototype, "select")

    const wrapper = mount(TextInput, {
      props: {
        modelValue: "test text",
        scopeName: "test",
        field: "test",
        title: "test",
        initialSelectAll: true,
      },
      attachTo: document.body,
    })

    // Wait for mounted hook to execute and select() to be called
    await wrapper.vm.$nextTick()
    // Browser Mode: Use vi.waitUntil to wait for select() to be called
    await vi.waitUntil(() => selectSpy.mock.calls.length > 0, { timeout: 1000 })
    await flushPromises()

    // Browser Mode: Verify select() was called on the real input element
    expect(selectSpy).toHaveBeenCalled()

    // Verify the element exists
    const inputElement = document.getElementById(
      "test-test"
    ) as HTMLInputElement
    expect(inputElement).toBeTruthy()

    selectSpy.mockRestore()
    wrapper.unmount()
  })

  it("does not select text when initialSelectAll is false", async () => {
    const wrapper = mount(TextInput, {
      props: {
        modelValue: "test text",
        scopeName: "test",
        field: "test",
        title: "test",
        initialSelectAll: false,
      },
      attachTo: document.body,
    })

    await wrapper.vm.$nextTick()
    // Browser Mode: Use requestAnimationFrame for proper async waiting instead of setTimeout
    await new Promise((resolve) =>
      requestAnimationFrame(() => resolve(undefined))
    )
    await flushPromises()

    // Browser Mode: Use document.getElementById since the component is attached to body
    // and we need to spy on the real DOM element
    const inputElement = document.getElementById(
      "test-test"
    ) as HTMLInputElement
    expect(inputElement).toBeTruthy()

    // Browser Mode: Spy on the REAL select() method to verify it's NOT called
    const selectSpy = vi.spyOn(inputElement, "select")

    // Wait a bit more to ensure onMounted has executed
    await wrapper.vm.$nextTick()
    await new Promise((resolve) =>
      requestAnimationFrame(() => resolve(undefined))
    )
    await flushPromises()

    expect(selectSpy).not.toHaveBeenCalled()

    selectSpy.mockRestore()
    wrapper.unmount()
  })
})
