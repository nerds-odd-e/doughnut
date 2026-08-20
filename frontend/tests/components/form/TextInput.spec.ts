import { mount, flushPromises } from "@vue/test-utils"
import TextInput from "@/components/form/TextInput.vue"
import { advanceAnimationFrame } from "@tests/helpers/focusTargetTestSupport"
import { afterEach, describe, expect, it, vi } from "vitest"

describe("TextInput.vue", () => {
  afterEach(() => {
    document.body.innerHTML = ""
    vi.useRealTimers()
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

  it("selects all text only when initialSelectAll is true", async () => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
    const selectSpy = vi.spyOn(HTMLInputElement.prototype, "select")

    const selected = mount(TextInput, {
      props: {
        modelValue: "test text",
        scopeName: "test",
        field: "selected",
        title: "test",
        initialSelectAll: true,
      },
      attachTo: document.body,
    })
    await flushPromises()
    await advanceAnimationFrame()

    expect(selectSpy).toHaveBeenCalled()
    const callsAfterSelectAll = selectSpy.mock.calls.length
    selected.unmount()

    const unselected = mount(TextInput, {
      props: {
        modelValue: "test text",
        scopeName: "test",
        field: "unselected",
        title: "test",
        initialSelectAll: false,
      },
      attachTo: document.body,
    })
    await flushPromises()
    await advanceAnimationFrame()

    expect(selectSpy.mock.calls.length).toBe(callsAfterSelectAll)
    selectSpy.mockRestore()
    unselected.unmount()
  })
})
