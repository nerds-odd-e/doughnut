import { describe, it, expect, vi } from "vitest"
import { render } from "@testing-library/vue"
import LoadingModal from "@/components/commons/LoadingModal.vue"

describe("LoadingModal", () => {
  it("should not render when show is false", () => {
    render(LoadingModal, {
      props: { show: false, message: "Loading..." },
    })
    // When show is false, Teleport doesn't render anything
    expect(document.querySelector(".loading-modal-mask")).toBeNull()
  })

  it("should render with spinner and message when show is true", () => {
    const { getByText, queryByText } = render(LoadingModal, {
      props: { show: true, message: "AI is creating note..." },
    })

    // Teleport renders to body, so we need to query document instead of container
    expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
    expect(document.querySelector(".daisy-loading-spinner")).toBeTruthy()
    expect(getByText("AI is creating note...")).toBeTruthy()
    expect(queryByText("Cancel")).toBeNull()
  })

  it("should render with default message when message prop is not provided", () => {
    const { getByText } = render(LoadingModal, {
      props: { show: true },
    })

    expect(getByText("Processing...")).toBeTruthy()
  })

  it("should not have close button", () => {
    render(LoadingModal, {
      props: { show: true, message: "Loading..." },
    })

    expect(document.querySelector(".close-button")).toBeNull()
  })

  it("renders one neutral native Cancel button only when an action is supplied", () => {
    const cancelAction = vi.fn()
    const { getByText } = render(LoadingModal, {
      props: {
        show: true,
        message: "AI is creating note...",
        loadingStateId: 11,
        cancelAction,
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
        loadingStateId: 21,
        cancelAction: firstCancel,
      },
    })
    const overlay = document.querySelector(".loading-modal-mask")
    const firstButton = getByText("Cancel")

    await rerender({
      show: true,
      message: "Older blocker",
      loadingStateId: 20,
      cancelAction: secondCancel,
    })

    const secondButton = getByText("Cancel")
    expect(document.querySelector(".loading-modal-mask")).toBe(overlay)
    expect(getByText("Older blocker")).toBeTruthy()
    expect(secondButton).not.toBe(firstButton)

    secondButton.click()
    expect(secondCancel).toHaveBeenCalledOnce()
    expect(firstCancel).not.toHaveBeenCalled()
  })

  it("keeps long messages centered with the optional action", () => {
    const longMessage = Array.from(
      { length: 20 },
      () => "Generating a detailed refinement layout"
    ).join(" ")
    const { getByText } = render(LoadingModal, {
      props: {
        show: true,
        message: longMessage,
        loadingStateId: 31,
        cancelAction: vi.fn(),
      },
    })

    const message = getByText(longMessage)
    expect(message).toHaveClass("loading-message")
    expect(getComputedStyle(message).textAlign).toBe("center")
    expect(getByText("Cancel")).toBeTruthy()
  })

  it("does not render the action while hidden", () => {
    const { queryByText } = render(LoadingModal, {
      props: {
        show: false,
        message: "Loading...",
        loadingStateId: 41,
        cancelAction: vi.fn(),
      },
    })

    expect(document.querySelector(".loading-modal-mask")).toBeNull()
    expect(queryByText("Cancel")).toBeNull()
  })
})
