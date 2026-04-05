import BookReadingPage from "@/pages/BookReadingPage.vue"
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

    const wrapper = helper
      .component(BookReadingPage)
      .withRouter()
      .withProps({ notebookId })
      .mount()
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

    const wrapper = helper
      .component(BookReadingPage)
      .withRouter()
      .withProps({ notebookId })
      .mount()
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

    const wrapper = helper
      .component(BookReadingPage)
      .withRouter()
      .withProps({ notebookId })
      .mount()
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
    const suffix = bookFileUrlSuffix(notebookId)
    vi.spyOn(globalThis, "fetch").mockImplementation((input, init) => {
      const url =
        typeof input === "string"
          ? input
          : input instanceof URL
            ? input.href
            : input.url
      if (!url.endsWith(suffix)) {
        return Promise.reject(new Error(`unexpected fetch: ${url}`))
      }
      expect(init?.credentials).toBe("same-origin")
      const copy = topMathsPdfBytes.slice(0)
      return Promise.resolve(
        new Response(copy, {
          status: 200,
          headers: { "Content-Type": "application/pdf" },
        })
      )
    })

    const wrapper = helper
      .component(BookReadingPage)
      .withRouter()
      .withProps({ notebookId })
      .mount()

    await vi.waitFor(
      () =>
        expect(wrapper.find('[data-testid="pdf-book-viewer"]').exists()).toBe(
          true
        ),
      { timeout: 15_000 }
    )
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

    const wrapper = helper
      .component(BookReadingPage)
      .withRouter()
      .withProps({ notebookId })
      .mount()

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
})
