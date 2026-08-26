import BookReadingPage from "@/pages/BookReadingPage.vue"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import createFetchMock from "vitest-fetch-mock"
import { expect, vi } from "vitest"
import { createMemoryHistory, createRouter } from "vue-router"
import epubMinimalUrl from "../../../e2e_test/fixtures/book_reading/epub_valid_minimal.epub?url"
import topMathsUrl from "../../../e2e_test/fixtures/book_reading/top-maths.pdf?url"

const fetchMock = createFetchMock(vi)

/** Keep in sync with `BookReadingPage.vue` */
export const CURRENT_BLOCK_ANCHOR_DEBOUNCE_MS = 120
export const LAST_READ_POSITION_PATCH_DEBOUNCE_MS = 400

export const notebookId = 7
/** Source file fetch uses book id, not notebook id — keep in sync with mocked `getBook` payloads. */
export const bookId = 701

let topMathsPdfBytes!: ArrayBuffer
let epubMinimalBytes!: ArrayBuffer

export function getTopMathsPdfBytes() {
  return topMathsPdfBytes
}

export function getEpubMinimalBytes() {
  return epubMinimalBytes
}

export async function loadBookReadingPageFixtures() {
  fetchMock.disableMocks()
  const res = await fetch(topMathsUrl)
  topMathsPdfBytes = await res.arrayBuffer()
  const epubRes = await fetch(epubMinimalUrl)
  epubMinimalBytes = await epubRes.arrayBuffer()
  fetchMock.enableMocks()
  fetchMock.doMock()
}

export function mockBookReadingPageDefaults() {
  mockSdkService(
    NotebookBooksController,
    "getNotebookBookReadingPosition",
    undefined
  )
  mockSdkService(NotebookBooksController, "getNotebookBookReadingRecords", [])
  mockSdkService(
    NotebookBooksController,
    "putNotebookBookBlockReadingRecord",
    []
  )
}

function bookFileUrlSuffix(id: number) {
  return `/api/books/${id}/file`
}

function fetchRequestUrl(input: RequestInfo | URL): string {
  return typeof input === "string"
    ? input
    : input instanceof URL
      ? input.href
      : input.url
}

export function mockNotebookBookFilePdfOk(
  id: number,
  pdfBytes: ArrayBuffer,
  options?: { assertSameOriginCredentials?: boolean }
) {
  const suffix = bookFileUrlSuffix(id)
  vi.spyOn(globalThis, "fetch").mockImplementation((input, init) => {
    const url = fetchRequestUrl(input)
    if (!url.endsWith(suffix)) {
      return Promise.reject(new Error(`unexpected fetch: ${url}`))
    }
    if (options?.assertSameOriginCredentials) {
      expect(init?.credentials).toBe("same-origin")
    }
    return Promise.resolve(
      new Response(pdfBytes.slice(0), {
        status: 200,
        headers: { "Content-Type": "application/pdf" },
      })
    )
  })
}

export function mockNotebookBookFileEpubOk(id: number, epubBytes: ArrayBuffer) {
  const suffix = bookFileUrlSuffix(id)
  vi.spyOn(globalThis, "fetch").mockImplementation((input, init) => {
    const url = fetchRequestUrl(input)
    if (!url.endsWith(suffix)) {
      return Promise.reject(new Error(`unexpected fetch: ${url}`))
    }
    expect(init?.credentials).toBe("same-origin")
    return Promise.resolve(
      new Response(epubBytes.slice(0), {
        status: 200,
        headers: { "Content-Type": "application/epub+zip" },
      })
    )
  })
}

export function createBookReadingPageRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/:pathMatch(.*)*", component: { template: "<div />" } }],
  })
}

export function mountBookReadingPage(id: number) {
  return helper
    .component(BookReadingPage)
    .withRouter(createBookReadingPageRouter())
    .withProps({ notebookId: id })
    .mount()
}

export type BookReadingPageWrapper = ReturnType<typeof mountBookReadingPage>

async function waitForTestId(wrapper: BookReadingPageWrapper, testId: string) {
  for (let attempt = 0; attempt < 40; attempt++) {
    await flushPromises()
    if (wrapper.find(`[data-testid="${testId}"]`).exists()) {
      return
    }
    await wrapper.vm.$nextTick()
  }
  expect(wrapper.find(`[data-testid="${testId}"]`).exists()).toBe(true)
}

export async function waitForPdfViewer(wrapper: BookReadingPageWrapper) {
  await waitForTestId(wrapper, "pdf-book-viewer")
}

export async function waitForEpubViewer(wrapper: BookReadingPageWrapper) {
  await waitForTestId(wrapper, "epub-book-viewer")
}

export async function withStubbedInnerWidth<T>(
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

export async function withFakeTimers(run: () => Promise<void>) {
  vi.useFakeTimers()
  try {
    await run()
  } finally {
    vi.useRealTimers()
  }
}

export function stubGetBookPlain(id: number) {
  return mockSdkService(
    NotebookBooksController,
    "getBook",
    makeMe.aBook.id(bookId).notebookId(String(id)).please()
  )
}

export function stubGetBookWithTopMathsBlocks(
  id: number,
  options?: {
    firstBlockHasNoDirectContent?: boolean
    lastBlockHasDirectContent?: boolean
  }
) {
  return mockSdkService(
    NotebookBooksController,
    "getBook",
    makeMe.aBook
      .id(bookId)
      .notebookId(String(id))
      .blocks(
        makeMe.bookReading.topMathsLikeFlatBlocks({
          firstBlockHasNoDirectContent: options?.firstBlockHasNoDirectContent,
          lastBlockHasDirectContent: options?.lastBlockHasDirectContent,
        })
      )
      .please()
  )
}

export function stubGetBookWithTopMathsLikeContentLocators(
  contentLocatorsForIndex: Parameters<
    typeof makeMe.bookReading.topMathsLikeBlockRows
  >[0]["contentLocatorsForIndex"],
  id: number = notebookId
) {
  return mockSdkService(
    NotebookBooksController,
    "getBook",
    makeMe.aBook
      .id(bookId)
      .notebookId(String(id))
      .blocks(
        makeMe.bookReading.topMathsLikeBlockRows({ contentLocatorsForIndex })
      )
      .please()
  )
}

export async function mountLoadedBookWithBlocks(
  id: number,
  options?: {
    innerWidth?: number
    assertSameOriginCredentials?: boolean
    firstBlockHasNoDirectContent?: boolean
    lastBlockHasDirectContent?: boolean
  }
) {
  stubGetBookWithTopMathsBlocks(id, {
    firstBlockHasNoDirectContent: options?.firstBlockHasNoDirectContent,
    lastBlockHasDirectContent: options?.lastBlockHasDirectContent,
  })
  mockNotebookBookFilePdfOk(bookId, topMathsPdfBytes, {
    assertSameOriginCredentials: options?.assertSameOriginCredentials,
  })
  const mount = async () => {
    const wrapper = mountBookReadingPage(id)
    await waitForPdfViewer(wrapper)
    return wrapper
  }
  if (options?.innerWidth !== undefined) {
    return withStubbedInnerWidth(options.innerWidth, mount)
  }
  return mount()
}
