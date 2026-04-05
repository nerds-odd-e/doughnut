import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import createFetchMock from "vitest-fetch-mock"
import { afterAll, beforeAll, describe, expect, it, vi } from "vitest"
import topMathsUrl from "../../../../e2e_test/fixtures/book_reading/top-maths.pdf?url"

const fetchMock = createFetchMock(vi)

describe("PdfBookViewer", () => {
  beforeAll(() => {
    fetchMock.disableMocks()
  })

  afterAll(() => {
    fetchMock.enableMocks()
    fetchMock.doMock()
  })

  it("mounts the viewer container for valid PDF bytes", async () => {
    const res = await fetch(topMathsUrl)
    const pdfBytes = await res.arrayBuffer()

    const wrapper = helper
      .component(PdfBookViewer)
      .withProps({ pdfBytes })
      .mount()

    await flushPromises()

    expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(true)
  })
})
