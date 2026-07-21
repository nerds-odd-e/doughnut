import { describe, it, expect, vi } from "vitest"
import { page } from "vitest/browser"
import { render } from "@testing-library/vue"
import LoadingModal from "@/components/commons/LoadingModal.vue"
import type { ApiLoadingCancelControl } from "@/managedApi/ApiStatusHandler"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import { nextTick } from "vue"

const pendingResult = { data: "pending" }
const neverSettles = () =>
  new Promise<typeof pendingResult>(() => {
    // The shared cancellation result settles independently of this request.
  })
const longLayoutMessage = Array.from(
  { length: 20 },
  () => "Generating a detailed refinement layout"
).join(" ")
const fullyVisible = (el: Element, height: number) => {
  const { top, bottom } = el.getBoundingClientRect()
  return top >= 0 && bottom <= height
}

describe("LoadingModal", () => {
  it("should not render when show is false", () => {
    render(LoadingModal, { props: { show: false, message: "Loading..." } })
    expect(document.querySelector(".loading-modal-mask")).toBeNull()
  })

  it("should render with spinner and message when show is true", () => {
    const { getByText, queryByText } = render(LoadingModal, {
      props: { show: true, message: "AI is creating note..." },
    })
    expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
    expect(document.querySelector(".daisy-loading-spinner")).toBeTruthy()
    expect(getByText("AI is creating note...")).toBeTruthy()
    expect(queryByText("Cancel")).toBeNull()
  })

  it("should render with default message when message prop is not provided", () => {
    const { getByText } = render(LoadingModal, { props: { show: true } })
    expect(getByText("Processing...")).toBeTruthy()
  })

  it("should not have close button", () => {
    render(LoadingModal, { props: { show: true, message: "Loading..." } })
    expect(document.querySelector(".close-button")).toBeNull()
  })

  it("requires cancel identity and action as one component control", () => {
    // @ts-expect-error a cancel action cannot be supplied without its identity
    const invalidControl: ApiLoadingCancelControl = { action: () => undefined }
    expect(invalidControl.action).toBeTypeOf("function")
  })

  it("renders one neutral native Cancel button only when a control is supplied", () => {
    const cancelAction = vi.fn()
    const { getByText } = render(LoadingModal, {
      props: {
        show: true,
        message: "AI is creating note...",
        cancelControl: { id: 11, action: cancelAction },
      },
    })
    const cancelButton = getByText("Cancel")
    expect(cancelButton.tagName).toBe("BUTTON")
    expect(cancelButton).toHaveAttribute("type", "button")
    expect(cancelButton).toHaveClass(
      "daisy-btn",
      "daisy-btn-ghost",
      "text-white"
    )
    expect(document.querySelectorAll("button")).toHaveLength(1)
    cancelButton.click()
    expect(cancelAction).toHaveBeenCalledOnce()
  })

  it("replaces only the identity-bound action when the selected state changes", async () => {
    const firstCancel = vi.fn()
    const secondCancel = vi.fn()
    const { getByText, rerender } = render(LoadingModal, {
      props: {
        show: true,
        message: "Newest blocker",
        cancelControl: { id: 21, action: firstCancel },
      },
    })
    const overlay = document.querySelector(".loading-modal-mask")
    const firstButton = getByText("Cancel")
    await rerender({
      show: true,
      message: "Older blocker",
      cancelControl: { id: 20, action: secondCancel },
    })
    const secondButton = getByText("Cancel")
    expect(document.querySelector(".loading-modal-mask")).toBe(overlay)
    expect(getByText("Older blocker")).toBeTruthy()
    expect(secondButton).not.toBe(firstButton)
    secondButton.click()
    expect(secondCancel).toHaveBeenCalledOnce()
    expect(firstCancel).not.toHaveBeenCalled()
  })

  it("preserves the existing message layout with the optional action", () => {
    const { getByText } = render(LoadingModal, {
      props: {
        show: true,
        message: longLayoutMessage,
        cancelControl: { id: 31, action: vi.fn() },
      },
    })
    const message = getByText(longLayoutMessage)
    expect(message).toHaveClass("loading-message")
    expect(getComputedStyle(message).textAlign).toBe("start")
    expect(getComputedStyle(message).maxWidth).toBe("none")
    expect(getComputedStyle(message).overflowWrap).toBe("normal")
    expect(getByText("Cancel")).toBeTruthy()
  })

  it("keeps a fitting long-message stack centered and a narrow one scrollable", async () => {
    const originalWidth = window.innerWidth
    const originalHeight = window.innerHeight
    const cancelAction = vi.fn()
    const { getByText, unmount } = render(LoadingModal, {
      props: {
        show: true,
        message: longLayoutMessage,
        cancelControl: { id: 51, action: cancelAction },
      },
    })
    try {
      await page.viewport(1280, 720)
      const overlay = document.querySelector(
        ".loading-modal-mask"
      ) as HTMLElement
      const content = document.querySelector(
        ".loading-modal-content"
      ) as HTMLElement
      const spinner = document.querySelector(".daisy-loading-spinner")!
      const cancelButton = getByText("Cancel")
      const wideOverlay = overlay.getBoundingClientRect()
      const wideContent = content.getBoundingClientRect()
      expect(wideContent.height).toBeLessThanOrEqual(wideOverlay.height)
      expect(
        Math.abs(
          wideContent.top -
            wideOverlay.top -
            (wideOverlay.bottom - wideContent.bottom)
        )
      ).toBeLessThan(2)

      await page.viewport(320, 568)
      overlay.scrollTop = 0
      expect(overlay.getBoundingClientRect().top).toBe(0)
      expect(content.getBoundingClientRect().top).toBeGreaterThanOrEqual(0)
      expect(fullyVisible(spinner, 568)).toBe(true)
      expect(overlay.scrollHeight).toBeGreaterThan(overlay.clientHeight)
      expect(overlay.scrollWidth).toBeLessThanOrEqual(overlay.clientWidth)
      overlay.scrollTop = overlay.scrollHeight - overlay.clientHeight
      expect(fullyVisible(cancelButton, 568)).toBe(true)
      cancelButton.click()
      expect(cancelAction).toHaveBeenCalledOnce()
    } finally {
      unmount()
      await page.viewport(originalWidth, originalHeight)
    }
  })

  it("does not render the action while hidden", () => {
    const { queryByText } = render(LoadingModal, {
      props: {
        show: false,
        message: "Loading...",
        cancelControl: { id: 41, action: vi.fn() },
      },
    })
    expect(document.querySelector(".loading-modal-mask")).toBeNull()
    expect(queryByText("Cancel")).toBeNull()
  })

  it("cancels the newest blocker and reveals the older action in the same overlay", async () => {
    const { getByText } = render(GlobalApiLoadingModal)
    const olderCall = apiCallWithLoading(() => neverSettles(), {
      blockUi: true,
      message: "Older cancelable blocker",
      cancelable: true,
    })
    const newestCall = apiCallWithLoading(() => neverSettles(), {
      blockUi: true,
      message: "Newest cancelable blocker",
      cancelable: true,
    })
    await nextTick()
    const overlay = document.querySelector(".loading-modal-mask")
    expect(getByText("Newest cancelable blocker")).toBeTruthy()
    const newestCancelButton = getByText("Cancel")
    newestCancelButton.click()
    await expect(newestCall).resolves.toEqual({ status: "cancelled" })
    await nextTick()
    expect(document.querySelector(".loading-modal-mask")).toBe(overlay)
    expect(getByText("Older cancelable blocker")).toBeTruthy()
    newestCancelButton.click()
    await nextTick()
    expect(getByText("Older cancelable blocker")).toBeTruthy()
    getByText("Cancel").click()
    await expect(olderCall).resolves.toEqual({ status: "cancelled" })
  })

  it("hides an older cancelable action behind the newest noncancelable blocker", async () => {
    const { getByText, queryByText } = render(GlobalApiLoadingModal)
    const olderCall = apiCallWithLoading(() => neverSettles(), {
      blockUi: true,
      message: "Older cancelable blocker",
      cancelable: true,
    })
    let resolveNewest: (value: typeof pendingResult) => void = () => undefined
    const newestCall = apiCallWithLoading(
      () =>
        new Promise<typeof pendingResult>((resolve) => {
          resolveNewest = resolve
        }),
      { blockUi: true, message: "Newest noncancelable blocker" }
    )
    await nextTick()
    expect(getByText("Newest noncancelable blocker")).toBeTruthy()
    expect(queryByText("Cancel")).toBeNull()
    resolveNewest(pendingResult)
    await newestCall
    await nextTick()
    expect(getByText("Older cancelable blocker")).toBeTruthy()
    getByText("Cancel").click()
    await expect(olderCall).resolves.toEqual({ status: "cancelled" })
  })
})
