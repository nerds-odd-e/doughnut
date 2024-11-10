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

  it("selects all text when initialSelectAll is true", async () => {
    // Mock select function
    const selectMock = vi.fn()

    // Create a mock input element
    const mockInput = document.createElement("input")
    mockInput.select = selectMock

    // Mock getElementById
    const getElementByIdSpy = vi.spyOn(document, "getElementById")
    getElementByIdSpy.mockReturnValue(mockInput)

    const wrapper = mount(TextInput, {
      props: {
        modelValue: "test text",
        scopeName: "test",
        field: "test",
        title: "test",
        initialSelectAll: true,
      },
    })

    // Wait for mounted hook to execute
    await wrapper.vm.$nextTick()

    expect(getElementByIdSpy).toHaveBeenCalledWith("test-test")
    expect(selectMock).toHaveBeenCalled()

    // Clean up
    getElementByIdSpy.mockRestore()
  })

  it("does not select text when initialSelectAll is false", async () => {
    const selectMock = vi.fn()

    // Create a mock input element
    const mockInput = document.createElement("input")
    mockInput.select = selectMock

    // Mock getElementById
    const getElementByIdSpy = vi.spyOn(document, "getElementById")
    getElementByIdSpy.mockReturnValue(mockInput)

    const wrapper = mount(TextInput, {
      props: {
        modelValue: "test text",
        scopeName: "test",
        field: "test",
        title: "test",
        initialSelectAll: false,
      },
    })

    await wrapper.vm.$nextTick()

    expect(selectMock).not.toHaveBeenCalled()

    // Clean up
    getElementByIdSpy.mockRestore()
  })
})
