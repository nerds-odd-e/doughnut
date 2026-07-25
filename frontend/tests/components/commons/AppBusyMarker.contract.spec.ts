import { describe, it, expect } from "vitest"
import { render } from "@testing-library/vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import LoadingThinBar from "@/components/commons/LoadingThinBar.vue"
import LoadingModal from "@/components/commons/LoadingModal.vue"

describe("data-app-busy contract", () => {
  it("marks ContentLoader as app-busy", () => {
    render(ContentLoader)
    expect(document.querySelector("[data-app-busy]")).toBeTruthy()
  })

  it("marks LoadingThinBar as app-busy", () => {
    render(LoadingThinBar)
    expect(document.querySelector("[data-app-busy]")).toBeTruthy()
  })

  it("marks LoadingModal as app-busy when shown", () => {
    render(LoadingModal, { props: { show: true, message: "Loading..." } })
    expect(document.querySelector("[data-app-busy]")).toBeTruthy()
  })

  it("does not mark LoadingModal as app-busy when hidden", () => {
    render(LoadingModal, { props: { show: false, message: "Loading..." } })
    expect(document.querySelector("[data-app-busy]")).toBeNull()
  })
})
