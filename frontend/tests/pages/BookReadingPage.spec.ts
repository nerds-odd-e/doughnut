import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import BookReadingPage from "@/pages/BookReadingPage.vue"
import type { BookRangeFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import helper, { wrapSdkResponse } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import createFetchMock from "vitest-fetch-mock"
import { beforeAll, beforeEach, describe, expect, it, vi } from "vitest"
import topMathsUrl from "../../../e2e_test/fixtures/book_reading/top-maths.pdf?url"

const fetchMock = createFetchMock(vi)

function bookFileUrlSuffix(id: number) {
  return `/api/notebooks/${id}/book/file`
}

function topMathsLikeFlatRanges(): BookRangeFull[] {
  const anchors = makeMe.bookReadingTopMathsLikeAnchors()
  return anchors.map((startAnchor, i) => ({
    id: i + 1,
    title: `Section ${i + 1}`,
    startAnchor,
    endAnchor: startAnchor,
    siblingOrder: i,
  }))
}

function fetchRequestUrl(input: RequestInfo | URL): string {
  return typeof input === "string"
    ? input
    : input instanceof URL
      ? input.href
      : input.url
}

function mockNotebookBookFilePdfOk(
  notebookId: number,
  pdfBytes: ArrayBuffer,
  options?: { assertSameOriginCredentials?: boolean }
) {
  const suffix = bookFileUrlSuffix(notebookId)
  vi.spyOn(globalThis, "fetch").mockImplementation((input, init) => {
    const url = fetchRequestUrl(input)
    if (!url.endsWith(suffix)) {
      return Promise.reject(new Error(`unexpected fetch: ${url}`))
    }
    if (options?.assertSameOriginCredentials) {
      expect(init?.credentials).toBe("same-origin")
    }
    const copy = pdfBytes.slice(0)
    return Promise.resolve(
      new Response(copy, {
        status: 200,
        headers: { "Content-Type": "application/pdf" },
      })
    )
  })
}

function mountBookReadingPage(notebookId: number) {
  return helper
    .component(BookReadingPage)
    .withRouter()
    .withProps({ notebookId })
    .mount()
}

type BookReadingPageWrapper = ReturnType<typeof mountBookReadingPage>

async function waitForPdfViewer(wrapper: BookReadingPageWrapper) {
  await vi.waitFor(
    () =>
      expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(
        true
      ),
    { timeout: 15_000 }
  )
}

async function withStubbedInnerWidth<T>(
  width: number,
  run: () => Promise<T>
): Promise<T> {
  const innerWidthDesc = Object.getOwnPropertyDescriptor(window, "innerWidth")
  Object.defineProperty(window, "innerWidth", {
    configurable: true,
    writable: true,
    value: width,
  })
  try {
    return await run()
  } finally {
    if (innerWidthDesc) {
      Object.defineProperty(window, "innerWidth", innerWidthDesc)
    }
  }
}

describe("BookReadingPage", () => {
  const notebookId = 7

  let topMathsPdfBytes: ArrayBuffer

  beforeAll(async () => {
    fetchMock.disableMocks()
    const res = await fetch(topMathsUrl)
    topMathsPdfBytes = await res.arrayBuffer()
    fetchMock.enableMocks()
    fetchMock.doMock()
  })

  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it("shows fetch error when book file returns an error status", async () => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(makeMe.aBook.please())
    )
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(null, { status: 404 })
    )

    const wrapper = mountBookReadingPage(notebookId)
    await flushPromises()

    const err = wrapper.find('[data-testid="book-reading-pdf-error"]')
    expect(err.exists()).toBe(true)
    expect(err.text()).toBe("Could not load the book file.")
    expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(false)
    expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(false)
  })

  it("does not load PDF viewer when hasSourceFile is false", async () => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(makeMe.aBook.hasSourceFile(false).please())
    )

    const wrapper = mountBookReadingPage(notebookId)
    await flushPromises()

    expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(false)
    expect(
      wrapper.find('[data-testid="book-reading-pdf-error"]').exists()
    ).toBe(false)
  })

  it("shows loading indicator while PDF is loading, hides it after render", async () => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(makeMe.aBook.please())
    )

    let resolveFetch!: (r: Response) => void
    vi.spyOn(globalThis, "fetch").mockReturnValue(
      new Promise<Response>((resolve) => {
        resolveFetch = resolve
      })
    )

    const wrapper = mountBookReadingPage(notebookId)
    await flushPromises()

    expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(true)
    expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(false)

    const copy = topMathsPdfBytes.slice(0)
    resolveFetch(
      new Response(copy, {
        status: 200,
        headers: { "Content-Type": "application/pdf" },
      })
    )

    await vi.waitFor(
      () => expect(wrapper.find(".daisy-loading-spinner").exists()).toBe(false),
      { timeout: 5000 }
    )
  })

  it("loads PDF into viewer when hasSourceFile is true", async () => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(makeMe.aBook.please())
    )
    mockNotebookBookFilePdfOk(notebookId, topMathsPdfBytes, {
      assertSameOriginCredentials: true,
    })

    const wrapper = mountBookReadingPage(notebookId)
    await waitForPdfViewer(wrapper)
  })

  it("shows error when PDF bytes are not valid", async () => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(makeMe.aBook.please())
    )
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(new TextEncoder().encode("not a pdf").buffer, {
        status: 200,
        headers: { "Content-Type": "text/plain" },
      })
    )

    const wrapper = mountBookReadingPage(notebookId)

    await vi.waitFor(
      () => {
        expect(
          wrapper.find('[data-testid="book-reading-pdf-error"]').exists()
        ).toBe(true)
      },
      { timeout: 15_000 }
    )

    expect(wrapper.find('[data-testid="book-reading-pdf-error"]').text()).toBe(
      "This file is not a valid PDF."
    )
    expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(false)
  })

  it("updates viewport-current outline row while outline drawer is closed (Phase 6.9)", async () => {
    await withStubbedInnerWidth(500, async () => {
      vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
        wrapSdkResponse(makeMe.aBook.ranges(topMathsLikeFlatRanges()).please())
      )
      mockNotebookBookFilePdfOk(notebookId, topMathsPdfBytes)

      const wrapper = mountBookReadingPage(notebookId)
      await waitForPdfViewer(wrapper)

      const pdf = wrapper.findComponent(PdfBookViewer)
      pdf.vm.$emit("viewportAnchorPage", {
        anchorPageIndexZeroBased: 0,
        viewport: null,
        pagesCount: 10,
      })

      await new Promise((r) => setTimeout(r, 200))

      const indicator = wrapper.find(
        '[data-testid="book-reading-page-indicator"]'
      )
      expect(indicator.exists()).toBe(true)
      expect(indicator.text().trim()).toBe("1 / 10")

      const current = wrapper.find('[data-outline-current="true"]')
      expect(current.exists()).toBe(true)
      expect(current.attributes("aria-current")).toBe("location")
      expect(current.text()).toBe("Section 3")
    })
  })

  it("zoom buttons exist with accessible names and page indicator shows via PdfControl (Phase 12)", async () => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(makeMe.aBook.ranges(topMathsLikeFlatRanges()).please())
    )
    mockNotebookBookFilePdfOk(notebookId, topMathsPdfBytes)

    const wrapper = mountBookReadingPage(notebookId)
    await waitForPdfViewer(wrapper)

    expect(
      wrapper.find('[data-testid="pdf-zoom-in"]').attributes("aria-label")
    ).toBe("Zoom in")
    expect(
      wrapper.find('[data-testid="pdf-zoom-out"]').attributes("aria-label")
    ).toBe("Zoom out")

    const pdf = wrapper.findComponent(PdfBookViewer)
    pdf.vm.$emit("viewportAnchorPage", {
      anchorPageIndexZeroBased: 0,
      viewport: null,
      pagesCount: 5,
    })
    await new Promise((r) => setTimeout(r, 200))

    const indicator = wrapper.find(
      '[data-testid="book-reading-page-indicator"]'
    )
    expect(indicator.exists()).toBe(true)
    expect(indicator.text().trim()).toBe("1 / 5")
  })

  it("outline toggle exposes aria-expanded and aria-controls (Phase 7.7)", async () => {
    await withStubbedInnerWidth(1024, async () => {
      vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
        wrapSdkResponse(makeMe.aBook.ranges(topMathsLikeFlatRanges()).please())
      )
      mockNotebookBookFilePdfOk(notebookId, topMathsPdfBytes)

      const wrapper = mountBookReadingPage(notebookId)
      await waitForPdfViewer(wrapper)

      const toggle = wrapper.find('[data-testid="book-reading-outline-toggle"]')
      const aside = wrapper.find('[data-testid="book-reading-outline-aside"]')
      expect(aside.attributes("id")).toBe("book-reading-outline-panel")
      expect(toggle.attributes("aria-controls")).toBe(
        "book-reading-outline-panel"
      )
      expect(toggle.attributes("aria-expanded")).toBe("true")

      await toggle.trigger("click")
      expect(toggle.attributes("aria-expanded")).toBe("false")

      await toggle.trigger("click")
      expect(toggle.attributes("aria-expanded")).toBe("true")
    })
  })
})
