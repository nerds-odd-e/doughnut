import { describe, it, expect } from "vitest"
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
    const { getByText } = render(LoadingModal, {
      props: { show: true, message: "AI is creating child note..." },
    })

    // Teleport renders to body, so we need to query document instead of container
    expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
    expect(document.querySelector(".daisy-loading-spinner")).toBeTruthy()
    expect(getByText("AI is creating child note...")).toBeTruthy()
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
})
