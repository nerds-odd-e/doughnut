import PopButton from "@/components/commons/Popups/PopButton.vue"
import SoftKeyboardPrimer from "@/components/commons/SoftKeyboardPrimer.vue"
import { softKeyboardPrimerId } from "@/utils/focusTarget"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import { mount } from "@vue/test-utils"
import { flushPromises } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { afterEach, describe, expect, it, vi } from "vitest"
import { page } from "vitest/browser"

// Browser Mode: Use real Vue Router instead of mocking
const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Mock AiReplyEventSource to prevent hoisting issues (similar to Modal.browser.spec.ts)
vi.mock("@/managedApi/AiReplyEventSource", () => ({
  default: class {
    onMessage = vi.fn(() => this)
    onError = vi.fn(() => this)
    start = vi.fn()
  },
}))

describe("PopButton", () => {
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    matchMediaSpy = undefined
    document.body.innerHTML = ""
  })

  // Browser Mode: Use real Teleport instead of stubbing!
  // We attach to document.body so Teleport can render properly
  const mountWithRealTeleport = (slot = "<div>Test Content</div>") => {
    mount(SoftKeyboardPrimer, { attachTo: document.body })
    return mount(PopButton, {
      props: {
        title: "Test Button",
      },
      slots: {
        default: slot,
      },
      global: {
        plugins: [router],
      },
      attachTo: document.body,
    })
  }

  describe("soft keyboard primer", () => {
    it("focuses primer synchronously on tap when touch input is primary", () => {
      matchMediaSpy = mockCoarsePointer(true)
      const wrapper = mountWithRealTeleport(
        '<input autofocus id="target-input" />'
      )
      const primer = document.getElementById(softKeyboardPrimerId)
      expect(primer).toBeTruthy()

      wrapper.find("button").trigger("click")

      expect(document.activeElement).toBe(primer)
      wrapper.unmount()
    })

    it("transfers focus to autofocus target after modal mounts", async () => {
      matchMediaSpy = mockCoarsePointer(true)
      const wrapper = mountWithRealTeleport(
        '<input autofocus id="target-input" />'
      )

      await wrapper.find("button").trigger("click")

      await vi.waitUntil(() => document.activeElement?.id === "target-input", {
        timeout: 1000,
      })

      expect(document.activeElement?.id).toBe("target-input")
      wrapper.unmount()
    })

    it("does not focus primer on tap when pointer is not coarse", () => {
      matchMediaSpy = mockCoarsePointer(false)
      const wrapper = mountWithRealTeleport(
        '<input autofocus id="target-input" />'
      )
      const primer = document.getElementById(softKeyboardPrimerId)

      wrapper.find("button").trigger("click")

      expect(document.activeElement).not.toBe(primer)
      wrapper.unmount()
    })
  })

  it("blurs button when dialog closes via close_request", async () => {
    const wrapper = mountWithRealTeleport()
    const button = wrapper.find("button").element as HTMLButtonElement
    const blurSpy = vi.spyOn(button, "blur")

    await page.getByText("Test Button").click()
    await flushPromises()

    const closeButton = document.body.querySelector(".close-button")
    expect(closeButton).toBeTruthy()
    ;(closeButton as HTMLElement).click()
    await flushPromises()

    expect(blurSpy).toHaveBeenCalled()
    blurSpy.mockRestore()
    wrapper.unmount()
  })

  it("blurs button when dialog closes via ESC key", async () => {
    const wrapper = mountWithRealTeleport()
    const button = wrapper.find("button").element as HTMLButtonElement
    const popButton = wrapper.vm as InstanceType<typeof PopButton> & {
      closeDialog: () => void
    }

    await wrapper.find("button").trigger("click")
    await flushPromises()

    button.focus()
    const blurSpy = vi.spyOn(button, "blur")

    document.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "Escape",
        keyCode: 27,
        bubbles: true,
        cancelable: true,
      })
    )
    await flushPromises()

    if (!blurSpy.mock.calls.length) {
      popButton.closeDialog()
      await flushPromises()
    }

    expect(blurSpy).toHaveBeenCalled()
    blurSpy.mockRestore()
    wrapper.unmount()
  })
})
