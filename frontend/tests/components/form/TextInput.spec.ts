import { mount } from "@vue/test-utils"
import TextInput from "@/components/form/TextInput.vue"

// Mock `getComputedStyle` to return a specific lineHeight
const originalGetComputedStyle = window.getComputedStyle
window.getComputedStyle = vi.fn().mockImplementation((elem) => ({
  ...originalGetComputedStyle(elem),
  lineHeight: "20px", // Example lineHeight
}))

describe("TextInput.vue", () => {
  // Reset mock after all tests are done
  afterAll(() => {
    window.getComputedStyle = originalGetComputedStyle
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
})
