import { mount, flushPromises } from "@vue/test-utils"
import TextInput from "@/components/form/TextInput.vue"
import { vi } from "vitest"

describe("TextInput.vue", () => {
  afterEach(() => {
    document.body.innerHTML = ""
  })

  it("disables the input when given the disabled prop", async () => {
    const wrapper = mount(TextInput, {
      props: {
        disabled: true,
        modelValue: "test",
        scopeName: "test",
        field: "test",
        title: "test",
      },
    })
    expect(wrapper.find("input").element.disabled).toBe(true)
  })

  it("selects all text when initialSelectAll is true", async () => {
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

    await wrapper.vm.$nextTick()
    await vi.waitUntil(() => selectSpy.mock.calls.length > 0, { timeout: 1000 })
    await flushPromises()

    expect(selectSpy).toHaveBeenCalled()
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
    await new Promise((resolve) =>
      requestAnimationFrame(() => resolve(undefined))
    )
    await flushPromises()

    const inputElement = document.getElementById(
      "test-test"
    ) as HTMLInputElement
    const selectSpy = vi.spyOn(inputElement, "select")

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
