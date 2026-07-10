import { softKeyboardPrimerId } from "@/utils/focusTarget"
import { flushPromises } from "@vue/test-utils"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import { afterEach, describe, expect, it, vi } from "vitest"
import {
  modalCloseButtonEl,
  mountPopButton,
  mountPopButtonWithPrimer,
  openPopButtonDialog,
  popButtonEl,
  waitForActiveElementId,
} from "./popButtonTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", () => ({
  default: class {},
}))

describe("PopButton", () => {
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    matchMediaSpy = undefined
    document.body.innerHTML = ""
  })

  describe("soft keyboard primer", () => {
    it("focuses primer synchronously on tap when touch input is primary", () => {
      matchMediaSpy = mockCoarsePointer(true)
      const wrapper = mountPopButtonWithPrimer(
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
      const wrapper = mountPopButtonWithPrimer(
        '<input autofocus id="target-input" />'
      )

      await openPopButtonDialog(wrapper)
      await waitForActiveElementId("target-input")

      expect(document.activeElement?.id).toBe("target-input")
      wrapper.unmount()
    })

    it("does not focus primer on tap when pointer is not coarse", () => {
      matchMediaSpy = mockCoarsePointer(false)
      const wrapper = mountPopButtonWithPrimer(
        '<input autofocus id="target-input" />'
      )
      const primer = document.getElementById(softKeyboardPrimerId)

      wrapper.find("button").trigger("click")

      expect(document.activeElement).not.toBe(primer)
      wrapper.unmount()
    })
  })

  it.each([
    {
      name: "close_request",
      closeDialog: async () => {
        const closeButton = modalCloseButtonEl()
        expect(closeButton).toBeTruthy()
        closeButton!.click()
        await flushPromises()
      },
    },
    {
      name: "ESC key",
      closeDialog: async () => {
        document.dispatchEvent(
          new KeyboardEvent("keydown", {
            key: "Escape",
            keyCode: 27,
            bubbles: true,
            cancelable: true,
          })
        )
        await flushPromises()
      },
    },
  ])("blurs button when dialog closes via $name", async ({ closeDialog }) => {
    const wrapper = mountPopButton()
    const button = popButtonEl(wrapper)
    const blurSpy = vi.spyOn(button, "blur")

    await openPopButtonDialog(wrapper)
    await closeDialog()

    expect(blurSpy).toHaveBeenCalled()
    blurSpy.mockRestore()
    wrapper.unmount()
  })
})
