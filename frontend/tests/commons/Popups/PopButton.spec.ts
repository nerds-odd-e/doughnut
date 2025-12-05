import PopButton from "@/components/commons/Popups/PopButton.vue"
import { mount } from "@vue/test-utils"
import { flushPromises } from "@vue/test-utils"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRoute: () => ({ path: "/" }),
  useRouter: () => ({
    push: mockedPush,
  }),
}))

describe("PopButton", () => {
  const mountWithoutTeleport = () =>
    mount(PopButton, {
      props: {
        title: "Test Button",
      },
      slots: {
        default: "<div>Test Content</div>",
      },
      global: {
        stubs: {
          Teleport: true,
        },
      },
    })

  it("blurs button when dialog closes via close_request", async () => {
    const wrapper = mountWithoutTeleport()
    const button = wrapper.find("button").element as HTMLButtonElement

    // Click button to open dialog
    await wrapper.find("button").trigger("click")
    await flushPromises()

    // Focus the button and verify it has focus
    button.focus()
    // In test environment, we check if blur was called by spying on it
    const blurSpy = vi.spyOn(button, "blur")

    // Close dialog by emitting close_request
    const modal = wrapper.findComponent({ name: "Modal" })
    await modal.vm.$emit("close_request")
    await flushPromises()

    // Button should have blur called
    expect(blurSpy).toHaveBeenCalled()
    blurSpy.mockRestore()
  })

  it("blurs button when dialog closes via ESC key", async () => {
    const wrapper = mountWithoutTeleport()
    const button = wrapper.find("button").element as HTMLButtonElement

    // Click button to open dialog
    await wrapper.find("button").trigger("click")
    await flushPromises()

    // Focus the button and spy on blur
    button.focus()
    const blurSpy = vi.spyOn(button, "blur")

    // Simulate ESC key press (Modal handles this since isPopup is not set)
    const escapeEvent = new KeyboardEvent("keydown", {
      key: "Escape",
      bubbles: true,
      cancelable: true,
    })
    document.dispatchEvent(escapeEvent)
    await flushPromises()

    // Button should have blur called
    expect(blurSpy).toHaveBeenCalled()
    blurSpy.mockRestore()
  })

  it("blurs button when closeDialog is called directly", async () => {
    const wrapper = mountWithoutTeleport()
    const button = wrapper.find("button").element as HTMLButtonElement
    const popButton = wrapper.vm as InstanceType<typeof PopButton>

    // Click button to open dialog
    await wrapper.find("button").trigger("click")
    await flushPromises()

    // Focus the button and spy on blur
    button.focus()
    const blurSpy = vi.spyOn(button, "blur")

    // Call closeDialog directly
    popButton.closeDialog()
    await flushPromises()

    // Button should have blur called
    expect(blurSpy).toHaveBeenCalled()
    blurSpy.mockRestore()
  })
})
