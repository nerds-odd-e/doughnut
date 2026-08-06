import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  expectSoftKeyboardPrimerIsFocused,
  expectSoftKeyboardPrimerIsNotFocused,
  mountSoftKeyboardPrimer,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import { flushPromises } from "@vue/test-utils"
import { afterEach, describe, expect, it, vi } from "vitest"
import {
  modalCloseButtonEl,
  mountPopButton,
  openPopButtonDialog,
  popButtonEl,
} from "./popButtonTestSupport"

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
      mountSoftKeyboardPrimer()
      const wrapper = mountPopButton('<input autofocus id="target-input" />')

      wrapper.find("button").trigger("click")

      expectSoftKeyboardPrimerIsFocused()
      wrapper.unmount()
    })

    it("does not focus primer on tap when pointer is not coarse", () => {
      matchMediaSpy = mockCoarsePointer(false)
      mountSoftKeyboardPrimer()
      const wrapper = mountPopButton('<input autofocus id="target-input" />')

      wrapper.find("button").trigger("click")

      expectSoftKeyboardPrimerIsNotFocused()
      wrapper.unmount()
    })
  })

  it.each([
    {
      name: "close_request",
      closeDialog: async () => {
        modalCloseButtonEl()!.click()
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
