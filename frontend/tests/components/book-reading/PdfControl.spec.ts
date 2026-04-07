import PdfControl from "@/components/book-reading/PdfControl.vue"
import helper from "@tests/helpers"
import { describe, expect, it } from "vitest"

describe("PdfControl", () => {
  it("renders page indicator when currentPage and pagesTotal are provided", () => {
    const wrapper = helper
      .component(PdfControl)
      .withProps({ currentPage: 3, pagesTotal: 10 })
      .mount()

    const indicator = wrapper.find(
      '[data-testid="book-reading-page-indicator"]'
    )
    expect(indicator.exists()).toBe(true)
    expect(indicator.text().trim()).toBe("3 / 10")
    expect(indicator.attributes("aria-label")).toBe("Page 3 of 10")
  })

  it("hides page indicator when currentPage is null", () => {
    const wrapper = helper
      .component(PdfControl)
      .withProps({ currentPage: null, pagesTotal: 10 })
      .mount()

    expect(
      wrapper.find('[data-testid="book-reading-page-indicator"]').exists()
    ).toBe(false)
  })

  it("hides page indicator when pagesTotal is null", () => {
    const wrapper = helper
      .component(PdfControl)
      .withProps({ currentPage: 1, pagesTotal: null })
      .mount()

    expect(
      wrapper.find('[data-testid="book-reading-page-indicator"]').exists()
    ).toBe(false)
  })

  it("emits zoomIn when zoom-in button is clicked", async () => {
    const wrapper = helper
      .component(PdfControl)
      .withProps({ currentPage: null, pagesTotal: null })
      .mount()

    await wrapper.find('[data-testid="pdf-zoom-in"]').trigger("click")

    expect(wrapper.emitted("zoomIn")).toHaveLength(1)
  })

  it("emits zoomOut when zoom-out button is clicked", async () => {
    const wrapper = helper
      .component(PdfControl)
      .withProps({ currentPage: null, pagesTotal: null })
      .mount()

    await wrapper.find('[data-testid="pdf-zoom-out"]').trigger("click")

    expect(wrapper.emitted("zoomOut")).toHaveLength(1)
  })
})
