import PdfFirstPageCanvas from "@/components/book-reading/PdfFirstPageCanvas.vue"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import createFetchMock from "vitest-fetch-mock"
import { afterAll, beforeAll, describe, expect, it, vi } from "vitest"
import topMathsUrl from "../../../../e2e_test/fixtures/book_reading/top-maths.pdf?url"

const fetchMock = createFetchMock(vi)

describe("PdfFirstPageCanvas", () => {
  beforeAll(() => {
    fetchMock.disableMocks()
  })

  afterAll(() => {
    fetchMock.enableMocks()
    fetchMock.doMock()
  })

  it("renders page 1 from PDF bytes", async () => {
    const res = await fetch(topMathsUrl)
    const pdfBytes = await res.arrayBuffer()

    const wrapper = helper
      .component(PdfFirstPageCanvas)
      .withProps({ pdfBytes })
      .mount()

    const deadline = Date.now() + 15_000
    let canvas: HTMLCanvasElement | undefined
    while (Date.now() < deadline) {
      await flushPromises()
      canvas = wrapper.find('[data-testid="pdf-first-page-canvas"]')
        .element as HTMLCanvasElement
      if (canvas.width > 0 && canvas.height > 0) break
      await new Promise((r) => setTimeout(r, 50))
    }

    expect(canvas?.width).toBeGreaterThan(0)
    expect(canvas?.height).toBeGreaterThan(0)
  })
})
